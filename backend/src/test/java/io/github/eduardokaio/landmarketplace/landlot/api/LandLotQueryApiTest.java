package io.github.eduardokaio.landmarketplace.landlot.api;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.TestcontainersConfiguration;
import io.github.eduardokaio.landmarketplace.auth.AccountFixtures;
import io.github.eduardokaio.landmarketplace.auth.infrastructure.AppUserRepository;
import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Read endpoints end to end through HTTP and PostGIS. Lots are squares on a grid of 0.001 degree
 * cells (about 110 m) around the search center; the search radius is 1000 m, about 9 cells.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LandLotQueryApiTest {

	private static final double CENTER_LON = -35.2;

	private static final double CENTER_LAT = -5.9;

	private static final double CELL = 0.001;

	private static final String RADIUS = "1000";

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
	void searchReturnsLotFullyInsideTheCircle() throws Exception {
		String inside = save(-1, -1, 1, 1);

		search(RADIUS).andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.type").value("FeatureCollection"))
			.andExpect(jsonPath("$.truncated").value(false))
			.andExpect(jsonPath("$.features[*].id", containsInAnyOrder(inside)))
			.andExpect(jsonPath("$.features[0].type").value("Feature"))
			.andExpect(jsonPath("$.features[0].properties.title").value("Lot"));
	}

	// The centroid is about 1100 m away, so only a real intersection test returns it
	@Test
	void searchReturnsLotCrossingTheCircleEdge() throws Exception {
		String crossing = save(8, -1, 12, 1);

		search(RADIUS).andExpect(jsonPath("$.features[*].id", containsInAnyOrder(crossing)));
	}

	@Test
	void searchSkipsLotOutsideTheCircle() throws Exception {
		String inside = save(-1, -1, 1, 1);
		String outside = save(20, -1, 22, 1);

		search(RADIUS).andExpect(jsonPath("$.features[*].id", containsInAnyOrder(inside)))
			.andExpect(jsonPath("$.features[*].id", not(hasItem(outside))));
	}

	// The lot is inside the bounding box of the circle (so the index returns it as a candidate)
	// but its nearest corner is about 1250 m away, so the exact distance check must drop it
	@Test
	void searchSkipsLotInTheCornerOfTheCircleBoundingBox() throws Exception {
		save(8, 8, 9, 9);

		search(RADIUS).andExpect(jsonPath("$.features.length()").value(0));
	}

	@Test
	void searchReturnsLotContainingTheWholeCircle() throws Exception {
		String around = save(-50, -50, 50, 50);

		search("10").andExpect(jsonPath("$.features[*].id", containsInAnyOrder(around)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-5", "abc" })
	void searchRejectsInvalidRadius(String radius) throws Exception {
		expectValidationError(get("/api/land-lots/search").param("lon", "-35.2").param("lat", "-5.9")
			.param("radius", radius), "radius");
	}

	@Test
	void searchRejectsMissingRadius() throws Exception {
		expectValidationError(get("/api/land-lots/search").param("lon", "-35.2").param("lat", "-5.9"), "radius");
	}

	@Test
	void searchRejectsRadiusAboveTheConfiguredMaximum() throws Exception {
		search("50000.1").andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/search-radius-too-large"))
			.andExpect(jsonPath("$.title").value("Search radius too large"))
			.andExpect(jsonPath("$.detail").value("The search radius must be at most 50000 meters."))
			.andExpect(jsonPath("$.maxRadiusMeters").value(50000.0));
	}

	@Test
	void searchAcceptsRadiusEqualToTheMaximum() throws Exception {
		search("50000").andExpect(status().isOk());
	}

	@ParameterizedTest
	@CsvSource({ "-181, -5.9, lon", "180.5, -5.9, lon", "abc, -5.9, lon", "NaN, -5.9, lon", "-35.2, 91, lat",
			"-35.2, -90.1, lat" })
	void searchRejectsCoordinatesOutOfRange(String lon, String lat, String field) throws Exception {
		expectValidationError(get("/api/land-lots/search").param("lon", lon).param("lat", lat).param("radius", RADIUS),
				field);
	}

	@Test
	void validationMessagesAreEnglishWhateverTheClientLanguage() throws Exception {
		mockMvc.perform(get("/api/land-lots/search").param("lon", "-35.2")
			.param("lat", "95")
			.param("radius", RADIUS)
			.header("Accept-Language", "pt-BR"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("must be less than or equal to 90"));
	}

	@Test
	void bboxReturnsLotsInTheViewport() throws Exception {
		String inside = save(-1, -1, 1, 1);
		String crossingEdge = save(8, -1, 12, 1);
		String outside = save(20, -1, 22, 1);

		mockMvc.perform(get("/api/land-lots").param("bbox", "-35.21,-5.91,-35.19,-5.89"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("FeatureCollection"))
			.andExpect(jsonPath("$.truncated").value(false))
			.andExpect(jsonPath("$.features[*].id", containsInAnyOrder(inside, crossingEdge)))
			.andExpect(jsonPath("$.features[*].id", not(hasItem(outside))));
	}

	@ParameterizedTest
	@ValueSource(strings = { "-35.21,-5.91,-35.19", "a,b,c,d", "-35.19,-5.91,-35.21,-5.89", "-35.21,-91,-35.19,-5.89",
			"-35.21,-5.91,-35.19,-5.89,1", "" })
	void bboxRejectsInvalidFormat(String bbox) throws Exception {
		mockMvc.perform(get("/api/land-lots").param("bbox", bbox))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/invalid-bounding-box"))
			.andExpect(jsonPath("$.title").value("Invalid bounding box"));
	}

	@Test
	void bboxIsRequired() throws Exception {
		mockMvc.perform(get("/api/land-lots"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void findsLotById() throws Exception {
		String id = save(-1, -1, 1, 1);

		mockMvc.perform(get("/api/land-lots/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("Feature"))
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.geometry.type").value("Polygon"))
			.andExpect(jsonPath("$.properties.status").value("AVAILABLE"));
	}

	@Test
	void unknownIdReturnsNotFoundProblem() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(get("/api/land-lots/{id}", id))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/land-lot-not-found"))
			.andExpect(jsonPath("$.title").value("Land lot not found"))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("No land lot with id " + id + "."));
	}

	@Test
	void malformedIdIsBadRequest() throws Exception {
		mockMvc.perform(get("/api/land-lots/{id}", "not-a-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	private ResultActions search(String radius) throws Exception {
		return mockMvc.perform(get("/api/land-lots/search").param("lon", String.valueOf(CENTER_LON))
			.param("lat", String.valueOf(CENTER_LAT))
			.param("radius", radius));
	}

	private void expectValidationError(MockHttpServletRequestBuilder request, String field) throws Exception {
		mockMvc.perform(request)
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/validation"))
			// NaN fails both @DecimalMin and @DecimalMax, so a field can have more than one error
			.andExpect(jsonPath("$.errors[*].field", everyItem(equalTo(field))))
			.andExpect(jsonPath("$.errors.length()", greaterThan(0)));
	}

	/** Saves a square from cell (x1, y1) to cell (x2, y2), relative to the search center. */
	private String save(int x1, int y1, int x2, int y2) {
		double minLon = CENTER_LON + x1 * CELL;
		double minLat = CENTER_LAT + y1 * CELL;
		double maxLon = CENTER_LON + x2 * CELL;
		double maxLat = CENTER_LAT + y2 * CELL;
		LandLot lot = LandLot.create("Lot", "Desc", new BigDecimal("100000.00"), LandLotFixtures.contact(),
				LandLotFixtures.polygon(minLon, minLat, maxLon, minLat, maxLon, maxLat, minLon, maxLat, minLon, minLat),
				ownerId);
		return repository.saveAndFlush(lot).getId().toString();
	}

}
