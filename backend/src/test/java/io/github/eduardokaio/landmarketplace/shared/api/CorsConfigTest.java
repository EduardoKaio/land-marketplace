package io.github.eduardokaio.landmarketplace.shared.api;

import java.util.List;

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
@Import({ GeoConfig.class, GeoJsonGeometryMapper.class, LandLotGeoJsonAssembler.class })
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
	void rejectsMethodsTheApiDoesNotHave() throws Exception {
		preflight(DEV_ORIGIN, "DELETE").andExpect(status().isForbidden());
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
