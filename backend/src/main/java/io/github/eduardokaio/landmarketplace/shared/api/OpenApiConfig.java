package io.github.eduardokaio.landmarketplace.shared.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General information of the OpenAPI document served at /v3/api-docs and /swagger-ui.html.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	@Bean
	OpenAPI landMarketplaceOpenApi() {
		return new OpenAPI().info(new Info().title("Land Marketplace API")
			.version("v1")
			.description("""
					Map-based land lot listing: register a lot by drawing its polygon, list the lots \
					in the visible map area and search the lots that intersect a circle.

					Coordinates are WGS84 longitude and latitude (EPSG:4326) and geometries are \
					GeoJSON (RFC 7946). Errors are Problem Details (RFC 9457) with the \
					application/problem+json media type."""));
	}

}
