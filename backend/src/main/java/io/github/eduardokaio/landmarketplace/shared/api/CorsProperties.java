package io.github.eduardokaio.landmarketplace.shared.api;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call the API, set under app.cors in application.yml.
 *
 * @param allowedOrigins exact origins such as http://localhost:5173; empty allows none
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
	}

}
