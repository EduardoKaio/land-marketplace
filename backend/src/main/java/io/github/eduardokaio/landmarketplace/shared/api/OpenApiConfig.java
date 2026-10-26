package io.github.eduardokaio.landmarketplace.shared.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General information of the OpenAPI document served at /v3/api-docs and /swagger-ui.html.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	@Bean
	OpenAPI landMarketplaceOpenApi() {
		// Operations that need a token reference this scheme, which enables the Authorize button
		SecurityScheme bearer = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");
		return new OpenAPI().components(new Components().addSecuritySchemes("bearer", bearer))
			.info(new Info().title("Land Marketplace API")
			.version("v1")
			.description("""
					Map-based land lot listing: register a lot by drawing its polygon, list the lots \
					in the visible map area and search the lots that intersect a circle.

					Reading is public. Creating, updating and deleting lots need an access token from \
					/api/auth/login or /api/auth/register, sent as "Authorization: Bearer <token>".

					Coordinates are WGS84 longitude and latitude (EPSG:4326) and geometries are \
					GeoJSON (RFC 7946). Errors are Problem Details (RFC 9457) with the \
					application/problem+json media type."""));
	}

}
