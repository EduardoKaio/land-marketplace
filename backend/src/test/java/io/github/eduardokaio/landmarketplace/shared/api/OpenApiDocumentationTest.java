package io.github.eduardokaio.landmarketplace.shared.api;

import io.github.eduardokaio.landmarketplace.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the generated OpenAPI document, so a refactoring cannot silently drop the documented
 * status codes or examples. Examples are asserted as parsed objects, which also proves they are
 * valid JSON. A single unnamed example is published as "example", named ones under "examples".
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocumentationTest {

	private static final String LOTS = "$.paths['/api/land-lots']";

	private static final String BY_ID = "$.paths['/api/land-lots/{id}']";

	private static final String PROBLEM = ".content['application/problem+json']";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void describesTheApi() throws Exception {
		apiDocs().andExpect(jsonPath("$.info.title").value("Land Marketplace API"))
			.andExpect(jsonPath("$.info.version").value("v1"));
	}

	@Test
	void documentsCreationWithExamplesAndEveryOutcome() throws Exception {
		apiDocs()
			.andExpect(jsonPath(LOTS + ".post.requestBody.content['application/json']"
					+ ".examples['Lot near Ponta Negra'].value.title")
				.value("Corner lot near Ponta Negra"))
			.andExpect(jsonPath(LOTS + ".post.responses['201'].headers.Location").exists())
			.andExpect(jsonPath(LOTS + ".post.responses['201'].content['application/json'].example.type")
				.value("Feature"))
			.andExpect(jsonPath(LOTS + ".post.responses['400']" + PROBLEM + ".example.status").value(400))
			.andExpect(jsonPath(LOTS + ".post.responses['409']" + PROBLEM + ".example.conflictingLotIds")
				.exists())
			.andExpect(jsonPath(LOTS + ".post.responses['422']" + PROBLEM + ".example.status").value(422));
	}

	@Test
	void documentsReadEndpoints() throws Exception {
		// Exactly these: the validation-only getters of LandLotFilterRequest must not become parameters
		apiDocs().andExpect(jsonPath(LOTS + ".get.parameters[*].name", containsInAnyOrder("bbox", "minPrice",
				"maxPrice", "minArea", "maxArea", "maxPricePerSquareMeter", "status")))
			.andExpect(jsonPath(LOTS + ".get.responses['400']" + PROBLEM).exists())
			.andExpect(jsonPath("$.paths['/api/land-lots/search'].get.parameters[*].name",
					containsInAnyOrder("lon", "lat", "radius", "minPrice", "maxPrice", "minArea", "maxArea",
							"maxPricePerSquareMeter", "status")))
			.andExpect(jsonPath(BY_ID + ".get.responses['400']" + PROBLEM).exists())
			.andExpect(jsonPath(BY_ID + ".get.responses['404']" + PROBLEM + ".example.status").value(404))
			.andExpect(jsonPath("$.paths['/api/land-lots/search'].get.responses['400']" + PROBLEM
					+ ".examples['Radius too large'].value.maxRadiusMeters")
				.value(50000.0));
	}

	private ResultActions apiDocs() throws Exception {
		return mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

}
