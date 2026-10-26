package io.github.eduardokaio.landmarketplace.auth.api;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/auth/login. No format rules: a wrong value is just invalid credentials. */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {

}
