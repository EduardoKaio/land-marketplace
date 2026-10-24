package io.github.eduardokaio.landmarketplace.shared.api;

import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.api.LandLotController;
import io.github.eduardokaio.landmarketplace.landlot.api.LandLotGeoJsonAssembler;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotQueryService;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotService;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotNotFoundException;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotOverlapException;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoConfig;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Error responses and error logging, through the web layer only: the services are mocks, so any
 * failure can be simulated without a database.
 */
@WebMvcTest(LandLotController.class)
@Import({ GeoConfig.class, GeoJsonGeometryMapper.class, LandLotGeoJsonAssembler.class })
@ExtendWith(OutputCaptureExtension.class)
class ApiErrorHandlingTest {

	private static final String STACK_FRAME = "\tat ";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LandLotService service;

	@MockitoBean
	private LandLotQueryService queryService;

	@Test
	void unexpectedErrorBecomesGenericProblemAndIsLoggedWithStackTrace(CapturedOutput output) throws Exception {
		UUID id = UUID.randomUUID();
		given(queryService.getById(id)).willThrow(new IllegalStateException("Connection to db-internal:5432 refused"));

		String body = mockMvc.perform(get("/api/land-lots/{id}", id))
			.andExpect(status().isInternalServerError())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/internal-error"))
			.andExpect(jsonPath("$.title").value("Internal Server Error"))
			.andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
			.andExpect(jsonPath("$.trace").doesNotExist())
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(body).doesNotContain("db-internal").doesNotContain("IllegalStateException");
		assertThat(output).contains("ERROR")
			.contains("GET /api/land-lots/" + id + " failed")
			.contains("java.lang.IllegalStateException: Connection to db-internal:5432 refused")
			.contains(STACK_FRAME);
	}

	@Test
	void expectedClientErrorIsLoggedAsOneWarningLine(CapturedOutput output) throws Exception {
		UUID id = UUID.randomUUID();
		given(queryService.getById(id)).willThrow(new LandLotNotFoundException(id));

		mockMvc.perform(get("/api/land-lots/{id}", id)).andExpect(status().isNotFound());

		assertThat(output).contains("WARN")
			.contains("GET /api/land-lots/" + id + " rejected with 404: LandLotNotFoundException")
			.doesNotContain(STACK_FRAME);
	}

	@Test
	void standardSpringErrorIsLoggedAsWarningWithoutTheExceptionMessage(CapturedOutput output) throws Exception {
		mockMvc.perform(get("/api/land-lots/{id}", "not-a-uuid"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

		assertThat(output).contains("WARN")
			.contains("GET /api/land-lots/not-a-uuid rejected with 400: MethodArgumentTypeMismatchException")
			.doesNotContain("Failed to convert")
			.doesNotContain(STACK_FRAME);
	}

	// The shared advice has a catch-all for Exception; the feature advice must still win
	@Test
	void featureErrorsAreNotSwallowedByTheCatchAll() throws Exception {
		UUID conflicting = UUID.randomUUID();
		given(service.create(any())).willThrow(new LandLotOverlapException(List.of(conflicting)));

		mockMvc.perform(post("/api/land-lots").contentType(MediaType.APPLICATION_JSON).content("""
				{"title": "Lot", "price": 1000, "description": "Desc",
				 "contact": {"name": "Kaio", "email": "kaio@example.com"},
				 "geometry": {"type": "Polygon", "coordinates": [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]]}}"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.type").value("/problems/land-lot-overlap"))
			.andExpect(jsonPath("$.conflictingLotIds[0]").value(conflicting.toString()));
	}

	@Test
	void unknownRouteIsAProblemToo() throws Exception {
		mockMvc.perform(get("/api/nothing-here"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

}
