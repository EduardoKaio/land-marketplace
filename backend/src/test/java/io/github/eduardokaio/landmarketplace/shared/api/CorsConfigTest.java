package io.github.eduardokaio.landmarketplace.shared.api;

import java.util.List;

import io.github.eduardokaio.landmarketplace.auth.infrastructure.SecurityConfig;
import io.github.eduardokaio.landmarketplace.landlot.api.LandLotController;
import io.github.eduardokaio.landmarketplace.landlot.api.LandLotGeoJsonAssembler;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotQueryService;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotService;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotSlice;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoConfig;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LandLotController.class)
// The slice does not pick up @Configuration classes, and without SecurityConfig Spring Boot's
// default security (everything authenticated) would apply
@Import({ SecurityConfig.class, GeoConfig.class, GeoJsonGeometryMapper.class, LandLotGeoJsonAssembler.class })
class CorsConfigTest {

	private static final String DEV_ORIGIN = "http://localhost:5173";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LandLotService service;

	@MockitoBean
	private LandLotQueryService queryService;

	@Test
	void allowsPreflightFromTheDevServer() throws Exception {
		preflight(DEV_ORIGIN, "POST").andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_ORIGIN))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
	}

	@Test
	void rejectsUnknownOrigin() throws Exception {
		preflight("http://evil.example", "GET").andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void allowsTheMethodsOfTheOwnerActions() throws Exception {
		preflight(DEV_ORIGIN, "PUT").andExpect(status().isOk());
		preflight(DEV_ORIGIN, "DELETE").andExpect(status().isOk());
	}

	// The frontend sends its token in this header, which is not one browsers allow by default
	@Test
	void allowsTheAuthorizationHeader() throws Exception {
		preflight(DEV_ORIGIN, "DELETE").andExpect(status().isOk());
		mockMvc
			.perform(options("/api/land-lots/{id}", "3f0c2a8e-5b1d-4c7a-9e2f-1a6b8d4c0e93")
				.header(HttpHeaders.ORIGIN, DEV_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")));
	}

	@Test
	void rejectsMethodsTheApiDoesNotHave() throws Exception {
		preflight(DEV_ORIGIN, "PATCH").andExpect(status().isForbidden());
	}

	// Without the exposed header the frontend could not read the URL of a created lot
	@Test
	void exposesTheLocationHeaderToTheFrontend() throws Exception {
		given(queryService.findInBoundingBox(any())).willReturn(new LandLotSlice(List.of(), false));

		mockMvc
			.perform(get("/api/land-lots").param("bbox", "-35.21,-5.91,-35.19,-5.89")
				.header(HttpHeaders.ORIGIN, DEV_ORIGIN))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_ORIGIN))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
	}

	private ResultActions preflight(String origin, String method) throws Exception {
		return mockMvc.perform(options("/api/land-lots").header(HttpHeaders.ORIGIN, origin)
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method));
	}

	@Nested
	@TestPropertySource(properties = "app.cors.allowed-origins=https://app.example,https://admin.example")
	class ConfiguredOrigins {

		@Test
		void acceptsEveryOriginOfTheCommaSeparatedList() throws Exception {
			preflight("https://app.example", "GET").andExpect(status().isOk());
			preflight("https://admin.example", "GET").andExpect(status().isOk());
		}

		@Test
		void replacesTheDefaultOrigin() throws Exception {
			preflight(DEV_ORIGIN, "GET").andExpect(status().isForbidden());
		}

	}

}
