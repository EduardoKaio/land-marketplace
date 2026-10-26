package io.github.eduardokaio.landmarketplace.auth.api;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the caller from a validated token. */
public final class CurrentUser {

	private CurrentUser() {
	}

	/** The subject is the user id; TokenService writes it, and the signature proves it was not changed. */
	public static UUID idOf(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}

}
