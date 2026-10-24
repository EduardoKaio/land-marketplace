package io.github.eduardokaio.landmarketplace.shared.api;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the frontend, served from another origin in development, call the API from the browser.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig implements WebMvcConfigurer {

	private final CorsProperties properties;

	public CorsConfig(CorsProperties properties) {
		this.properties = properties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			// Exact origins, never "*": any site could otherwise call the API from a user's browser
			.allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
			// Only the methods the API has; add more when endpoints need them
			.allowedMethods("GET", "POST")
			// Browsers hide response headers from cross-origin scripts unless they are listed here;
			// the frontend reads Location after a 201
			.exposedHeaders(HttpHeaders.LOCATION)
			// Lets the browser cache the preflight answer for an hour
			.maxAge(3600);
	}

}
