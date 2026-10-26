package io.github.eduardokaio.landmarketplace.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;

/**
 * OpenAPI documentation of the authentication endpoints, kept apart from the controller.
 */
@Tag(name = "Authentication", description = "Create an account and get an access token")
interface AuthApi {

	String PROBLEM_JSON = "application/problem+json";

	@Operation(summary = "Create an account",
			description = "Creates the account and returns an access token, so no separate login is needed. "
					+ "The password needs 8 to 64 characters with at least one letter and one digit.")
	@ApiResponse(responseCode = "201", description = "Account created and logged in")
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields, including a weak password",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "An account with this email already exists",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
	TokenResponse register(RegisterRequest request);

	@Operation(summary = "Log in",
			description = "Returns an access token to send as \"Authorization: Bearer <token>\".")
	@ApiResponse(responseCode = "200", description = "Logged in")
	@ApiResponse(responseCode = "401", description = "Unknown email or wrong password",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
	TokenResponse login(LoginRequest request);

}
