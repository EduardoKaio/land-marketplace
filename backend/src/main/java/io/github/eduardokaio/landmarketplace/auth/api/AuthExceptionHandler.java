package io.github.eduardokaio.landmarketplace.auth.api;

import java.net.URI;

import io.github.eduardokaio.landmarketplace.auth.domain.AccountNotFoundException;
import io.github.eduardokaio.landmarketplace.auth.domain.EmailAlreadyRegisteredException;
import io.github.eduardokaio.landmarketplace.auth.domain.InvalidCredentialsException;
import io.github.eduardokaio.landmarketplace.shared.api.ApiExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Authentication errors as ProblemDetail, including the 401 and 403 of the security filters (see
 * ProblemDetailAuthenticationEntryPoint). First in order for the same reason as
 * LandLotExceptionHandler: the shared advice has a catch-all.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AuthExceptionHandler {

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex, HttpServletRequest request) {
		ApiExceptionHandler.logRejected(request, HttpStatus.CONFLICT, ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setType(URI.create("/problems/email-already-registered"));
		problem.setTitle("Email already registered");
		return problem;
	}

	@ExceptionHandler({ InvalidCredentialsException.class, AccountNotFoundException.class })
	ProblemDetail handleRejectedCredentials(RuntimeException ex, HttpServletRequest request) {
		ApiExceptionHandler.logRejected(request, HttpStatus.UNAUTHORIZED, ex);
		return unauthorized(ex.getMessage());
	}

	/** No token, or a token that is malformed, expired, wrongly signed or from another issuer. */
	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		ApiExceptionHandler.logRejected(request, HttpStatus.UNAUTHORIZED, ex);
		boolean tokenSent = request.getHeader("Authorization") != null;
		// The exception message may describe the token; the client only needs to know what to do
		return unauthorized(tokenSent ? "The access token is invalid or expired. Please log in again."
				: "Authentication is required. Send a bearer access token.");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		ApiExceptionHandler.logRejected(request, HttpStatus.FORBIDDEN, ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"You do not have permission to perform this action.");
		problem.setType(URI.create("/problems/forbidden"));
		problem.setTitle("Forbidden");
		return problem;
	}

	private static ProblemDetail unauthorized(String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
		problem.setType(URI.create("/problems/unauthorized"));
		problem.setTitle("Unauthorized");
		return problem;
	}

}
