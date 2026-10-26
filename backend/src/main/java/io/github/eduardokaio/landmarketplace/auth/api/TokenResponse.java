package io.github.eduardokaio.landmarketplace.auth.api;

import java.time.Clock;
import java.time.Duration;

import io.github.eduardokaio.landmarketplace.auth.application.AccessToken;

/**
 * Answer of register and login, shaped like an OAuth 2 token response (RFC 6749, section 5.1)
 * plus the user, so the client can show who is logged in without decoding the token.
 *
 * @param expiresIn seconds until the token expires
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

	static TokenResponse from(AccessToken token, Clock clock) {
		long expiresIn = Duration.between(clock.instant(), token.expiresAt()).toSeconds();
		return new TokenResponse(token.value(), "Bearer", expiresIn, UserResponse.from(token.user()));
	}

}
