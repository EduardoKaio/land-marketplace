package io.github.eduardokaio.landmarketplace.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/auth/register. The password limit of 64 characters keeps it under the 72
 * bytes BCrypt reads; longer passwords would be silently cut.
 */
public record RegisterRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Email @Size(max = 254) String email,
		@NotNull @Size(min = 8, max = 64)
		@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).*$", message = "must contain at least one letter and one digit")
		String password) {

}
