package io.github.eduardokaio.landmarketplace.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Access token settings, under app.jwt in application.yml.
 *
 * @param secret HMAC key of HS256. At least 32 bytes: HS256 needs a 256 bit key, and a shorter
 * one would make the startup fail later with a less clear error
 * @param expiration how long an access token is valid
 * @param issuer written to the "iss" claim and required back when a token is validated
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {

	static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException("app.jwt.secret must have at least " + MIN_SECRET_BYTES + " bytes");
		}
		if (expiration == null || expiration.isNegative() || expiration.isZero()) {
			throw new IllegalArgumentException("app.jwt.expiration must be positive");
		}
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("app.jwt.issuer is required");
		}
	}

}
