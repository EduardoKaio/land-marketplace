package io.github.eduardokaio.landmarketplace.landlot.api;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import io.github.eduardokaio.landmarketplace.TestcontainersConfiguration;
import io.github.eduardokaio.landmarketplace.auth.AccountFixtures;
import io.github.eduardokaio.landmarketplace.auth.infrastructure.AppUserRepository;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End to end through HTTP, the service and PostGIS. Polygons are axis-aligned squares on a grid of
 * 0.001 degree cells near Ponta Negra; the existing lot is the square from cell (0, 0) to (10, 10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LandLotControllerTest {

	private static final double ORIGIN_LON = -35.2;

	private static final double ORIGIN_LAT = -5.9;

	private static final double CELL = 0.001;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LandLotRepository repository;

	@Autowired
	private AppUserRepository accounts;

	private UUID ownerId;

	@BeforeEach
	void cleanDatabase() {
		// Lots first: they reference their owners
		repository.deleteAll();
		accounts.deleteAll();
		ownerId = accounts.save(AccountFixtures.account("owner@example.com")).getId();
	}

	@Test
	void createsLotAndReturnsFeatureWithLocation() throws Exception {
		MvcResult result = submit(square(0, 0, 10, 10)).andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.type").value("Feature"))
			.andExpect(jsonPath("$.geometry.type").value("Polygon"))
			.andExpect(jsonPath("$.properties.title").value("Corner lot"))
			.andExpect(jsonPath("$.properties.price").value(250000.00))
			.andExpect(jsonPath("$.properties.areaSquareMeters").isNumber())
			.andExpect(jsonPath("$.properties.pricePerSquareMeter").isNumber())
			.andExpect(jsonPath("$.properties.status").value("AVAILABLE"))
			.andExpect(jsonPath("$.properties.contact.email").value("kaio@example.com"))
			.andReturn();

		String id = idOf(result);
		assertThat(result.getResponse().getHeader("Location")).endsWith("/api/land-lots/" + id);
		assertThat(repository.findById(UUID.fromString(id))).get()
			.satisfies(lot -> assertThat(lot.getOwnerId()).isEqualTo(ownerId));
	}

	@Test
	void rejectsPartialOverlap() throws Exception {
		String existing = createExisting(square(0, 0, 10, 10));

		expectConflict(square(5, 5, 15, 15), existing);
	}

	@Test
	void rejectsNewLotInsideExistingLot() throws Exception {
		String existing = createExisting(square(0, 0, 10, 10));

		expectConflict(square(2, 2, 8, 8), existing);
	}

	@Test
	void rejectsNewLotContainingExistingLot() throws Exception {
		String existing = createExisting(square(0, 0, 10, 10));

		expectConflict(square(-5, -5, 15, 15), existing);
	}

	@Test
	void rejectsIdenticalGeometry() throws Exception {
		String existing = createExisting(square(0, 0, 10, 10));

		expectConflict(square(0, 0, 10, 10), existing);
	}

	@Test
	void listsEveryConflictingLot() throws Exception {
		String left = createExisting(square(0, 0, 10, 10));
		String right = createExisting(square(20, 0, 30, 10));
		List<String> sortedIds = Stream.of(left, right).sorted().toList();

		submit(square(5, 0, 25, 10)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("The submitted polygon overlaps 2 existing land lots."))
			.andExpect(jsonPath("$.conflictingLotIds[0]").value(sortedIds.get(0)))
			.andExpect(jsonPath("$.conflictingLotIds[1]").value(sortedIds.get(1)));
	}

	@Test
	void acceptsNeighborSharingABorder() throws Exception {
		createExisting(square(0, 0, 10, 10));

		submit(square(10, 0, 20, 10)).andExpect(status().isCreated());
	}

	@Test
	void acceptsNeighborTouchingOnlyAtAVertex() throws Exception {
		createExisting(square(0, 0, 10, 10));

		submit(square(10, 10, 20, 20)).andExpect(status().isCreated());
	}

	@Test
	void acceptsDisjointLot() throws Exception {
		createExisting(square(0, 0, 10, 10));

		submit(square(50, 50, 60, 60)).andExpect(status().isCreated());
	}

	@Test
	void acceptsLotInsideTheHoleOfAnExistingLot() throws Exception {
		createExisting(polygonWithHole());

		submit(square(4, 4, 6, 6)).andExpect(status().isCreated());
	}

	@Test
	void rejectsBowTieWithReason() throws Exception {
		String bowTie = geometry("[[%s, %s, %s, %s, %s]]".formatted(position(0, 0), position(10, 10), position(10, 0),
				position(0, 10), position(0, 0)));

		submit(bowTie).andExpect(status().isUnprocessableContent())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/invalid-geometry"))
			.andExpect(jsonPath("$.title").value("Invalid geometry"))
			.andExpect(jsonPath("$.detail").value(containsString("Self-intersection")));
	}

	@Test
	void rejectsGeometryThatIsNotAPolygon() throws Exception {
		submit("{\"type\": \"Point\", \"coordinates\": [-35.2, -5.9]}").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Geometry type must be Polygon, got: \"Point\""));
	}

	@Test
	void rejectsInvalidPayloadListingEveryField() throws Exception {
		String body = """
				{"title": " ", "price": -1, "description": "Desc",
				 "contact": {"name": "Kaio"}, "geometry": %s}""".formatted(square(0, 0, 10, 10));

		submitBody(body).andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/validation"))
			.andExpect(jsonPath("$.title").value("Invalid request"))
			.andExpect(jsonPath("$.errors[0].field").value("contact.phoneOrEmailPresent"))
			.andExpect(jsonPath("$.errors[1].field").value("price"))
			.andExpect(jsonPath("$.errors[2].field").value("title"))
			.andExpect(jsonPath("$.errors.length()").value(3));
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		submitBody("{\"title\": ").andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	private void expectConflict(String geometry, String existingId) throws Exception {
		submit(geometry).andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/land-lot-overlap"))
			.andExpect(jsonPath("$.title").value("Land lot overlaps existing lots"))
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.detail").value("The submitted polygon overlaps 1 existing land lot."))
			.andExpect(jsonPath("$.conflictingLotIds.length()").value(1))
			.andExpect(jsonPath("$.conflictingLotIds[0]").value(existingId));
		assertThat(repository.count()).isEqualTo(1);
	}

	private String createExisting(String geometry) throws Exception {
		return idOf(submit(geometry).andExpect(status().isCreated()).andReturn());
	}

	private static String idOf(MvcResult result) throws Exception {
		return JsonMapper.shared().readTree(result.getResponse().getContentAsString()).get("id").stringValue();
	}

	private ResultActions submit(String geometry) throws Exception {
		return submitBody("""
				{"title": "Corner lot", "price": 250000.00, "description": "Flat terrain",
				 "contact": {"name": "Kaio Eduardo", "phone": "(84) 99999-0000", "email": "kaio@example.com"},
				 "geometry": %s}""".formatted(geometry));
	}

	private ResultActions submitBody(String body) throws Exception {
		// jwt() puts an authenticated token in the security context without signing one; the access
		// rules themselves are tested in LandLotAccessTest
		return mockMvc.perform(post("/api/land-lots").with(jwt().jwt(token -> token.subject(ownerId.toString())))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private static String square(int x1, int y1, int x2, int y2) {
		return geometry("[%s]".formatted(ring(x1, y1, x2, y2)));
	}

	private static String polygonWithHole() {
		return geometry("[%s, %s]".formatted(ring(0, 0, 10, 10), ring(2, 2, 8, 8)));
	}

	private static String ring(int x1, int y1, int x2, int y2) {
		return "[%s, %s, %s, %s, %s]".formatted(position(x1, y1), position(x2, y1), position(x2, y2),
				position(x1, y2), position(x1, y1));
	}

	private static String position(int x, int y) {
		return String.format(Locale.ROOT, "[%.7f, %.7f]", ORIGIN_LON + x * CELL, ORIGIN_LAT + y * CELL);
	}

	private static String geometry(String coordinates) {
		return "{\"type\": \"Polygon\", \"coordinates\": " + coordinates + "}";
	}

}
