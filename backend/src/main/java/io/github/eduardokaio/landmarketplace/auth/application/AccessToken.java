package io.github.eduardokaio.landmarketplace.auth.application;

import java.time.Instant;

import io.github.eduardokaio.landmarketplace.auth.domain.AppUser;

/** A signed token and the account it was issued to. */
public record AccessToken(String value, Instant expiresAt, AppUser user) {

}
