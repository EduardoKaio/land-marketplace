package io.github.eduardokaio.landmarketplace.landlot.api;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.TestcontainersConfiguration;
import io.github.eduardokaio.landmarketplace.auth.AccountFixtures;
import io.github.eduardokaio.landmarketplace.auth.infrastructure.AppUserRepository;
import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotStatus;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One test per access rule: reads are public, writes need a token, and only the owner changes or
 * deletes a lot. jwt() stands for a validated token of the given account; signing and validating
 * real tokens is covered by AuthControllerTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LandLotAccessTest {

	private static final String UPDATE = """
			{"title": "Sold corner lot", "price": 240000.00, "description": "Sold last week",
			 "contact": {"name": "Kaio", "email": "kaio@example.com"}, "status": "SOLD"}""";

	private static final String CREATE = """
			{"title": "Corner lot", "price": 250000.00, "description": "Flat terrain",
			 "contact": {"name": "Kaio", "email": "kaio@example.com"},
			 "geometry": {"type": "Polygon", "coordinates":
			   [[[-35.2, -5.9], [-35.199, -5.9], [-35.199, -5.899], [-35.2, -5.899], [-35.2, -5.9]]]}}""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LandLotRepository lots;

	@Autowired
	private AppUserRepository accounts;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private TransactionTemplate transaction;

	private UUID ownerId;

	private UUID strangerId;

	private UUID lotId;

	@BeforeEach
	void setUp() {
		lots.deleteAll();
		accounts.deleteAll();
		ownerId = accounts.save(AccountFixtures.account("owner@example.com")).getId();
		strangerId = accounts.save(AccountFixtures.account("stranger@example.com")).getId();
		lotId = lots.save(LandLotFixtures.pontaNegraLot(ownerId)).getId();
	}

	@Test
	void anyoneReadsWithoutAToken() throws Exception {
		mockMvc.perform(get("/api/land-lots/{id}", lotId)).andExpect(status().isOk());
		mockMvc.perform(get("/api/land-lots").param("bbox", "-35.3,-6.0,-35.1,-5.8")).andExpect(status().isOk());
		mockMvc.perform(get("/api/land-lots/search").param("lon", "-35.1955").param("lat", "-5.8795")
			.param("radius", "100")).andExpect(status().isOk());
	}

	@Test
	void creatingNeedsAToken() throws Exception {
		expectUnauthorized(mockMvc.perform(json(post("/api/land-lots"), CREATE)));
		assertThat(lots.count()).isEqualTo(1);
	}

	@Test
	void creatingWithTheTokenOfADeletedAccountIsUnauthorized() throws Exception {
		mockMvc.perform(json(post("/api/land-lots"), CREATE).with(as(UUID.randomUUID())))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("The account of this session no longer exists. Please log in again."));
	}

	@Test
	void ownerUpdatesTheDetailsButNotTheGeometry() throws Exception {
		String withGeometry = UPDATE.replace("\"status\": \"SOLD\"}",
				"\"status\": \"SOLD\", \"geometry\": {\"type\": \"Point\", \"coordinates\": [0, 0]}}");

		mockMvc.perform(json(put("/api/land-lots/{id}", lotId), withGeometry).with(as(ownerId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.properties.title").value("Sold corner lot"))
			.andExpect(jsonPath("$.properties.price").value(240000.00))
			.andExpect(jsonPath("$.properties.status").value("SOLD"))
			.andExpect(jsonPath("$.geometry.type").value("Polygon"));

		LandLot saved = lots.findById(lotId).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(LandLotStatus.SOLD);
		assertThat(saved.getGeom().equalsExact(LandLotFixtures.pontaNegraSquare())).isTrue();
	}

	@Test
	void onlyTheOwnerUpdates() throws Exception {
		expectForbidden(mockMvc.perform(json(put("/api/land-lots/{id}", lotId), UPDATE).with(as(strangerId))));
		assertThat(lots.findById(lotId).orElseThrow().getTitle()).isEqualTo("Corner lot near Ponta Negra");
	}

	@Test
	void updatingNeedsAToken() throws Exception {
		expectUnauthorized(mockMvc.perform(json(put("/api/land-lots/{id}", lotId), UPDATE)));
	}

	@Test
	void updatingAMissingLotIsNotFound() throws Exception {
		mockMvc.perform(json(put("/api/land-lots/{id}", UUID.randomUUID()), UPDATE).with(as(ownerId)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.type").value("/problems/land-lot-not-found"));
	}

	@Test
	void updateValidatesEveryField() throws Exception {
		mockMvc.perform(json(put("/api/land-lots/{id}", lotId), """
				{"title": " ", "price": 1, "description": "Desc", "contact": {"name": "Kaio", "phone": "1"}}""")
			.with(as(ownerId)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("status"))
			.andExpect(jsonPath("$.errors[1].field").value("title"));
	}

	// Lots created before accounts existed have no owner: still public, but nobody can change them
	@Test
	void lotsWithoutOwnerCannotBeChangedByAnyone() throws Exception {
		transaction.executeWithoutResult(status -> entityManager
			.createNativeQuery("UPDATE land_lot SET owner_id = NULL WHERE id = :id")
			.setParameter("id", lotId)
			.executeUpdate());

		expectForbidden(mockMvc.perform(json(put("/api/land-lots/{id}", lotId), UPDATE).with(as(ownerId))));
		expectForbidden(mockMvc.perform(delete("/api/land-lots/{id}", lotId).with(as(ownerId))));
	}

	@Test
	void ownerDeletes() throws Exception {
		mockMvc.perform(delete("/api/land-lots/{id}", lotId).with(as(ownerId))).andExpect(status().isNoContent());

		assertThat(lots.existsById(lotId)).isFalse();
	}

	@Test
	void onlyTheOwnerDeletes() throws Exception {
		expectForbidden(mockMvc.perform(delete("/api/land-lots/{id}", lotId).with(as(strangerId))));
		assertThat(lots.existsById(lotId)).isTrue();
	}

	@Test
	void deletingNeedsAToken() throws Exception {
		expectUnauthorized(mockMvc.perform(delete("/api/land-lots/{id}", lotId)));
		assertThat(lots.existsById(lotId)).isTrue();
	}

	@Test
	void myLotsListsOnlyTheLotsOfTheCaller() throws Exception {
		lots.save(LandLot.create("Stranger lot", "Desc", new BigDecimal("1000.00"), LandLotFixtures.contact(),
				LandLotFixtures.polygon(-35.0, -5.9, -34.999, -5.9, -34.999, -5.899, -35.0, -5.899, -35.0, -5.9),
				strangerId));

		mockMvc.perform(get("/api/me/land-lots").with(as(ownerId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("FeatureCollection"))
			.andExpect(jsonPath("$.features.length()").value(1))
			.andExpect(jsonPath("$.features[0].id").value(lotId.toString()))
			.andExpect(jsonPath("$.truncated").value(false));
	}

	@Test
	void myLotsNeedsAToken() throws Exception {
		expectUnauthorized(mockMvc.perform(get("/api/me/land-lots")));
	}

	private static RequestPostProcessor as(UUID accountId) {
		return jwt().jwt(token -> token.subject(accountId.toString()));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
		return request.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static void expectUnauthorized(ResultActions result) throws Exception {
		result.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/unauthorized"));
	}

	private static void expectForbidden(ResultActions result) throws Exception {
		result.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/not-land-lot-owner"))
			.andExpect(jsonPath("$.title").value("Not the owner of the land lot"));
	}

}
