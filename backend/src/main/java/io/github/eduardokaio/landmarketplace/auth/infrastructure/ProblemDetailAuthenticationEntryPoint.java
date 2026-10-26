package io.github.eduardokaio.landmarketplace.auth.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Answers 401 when a request has no token or an invalid one.
 * <p>
 * Security filters run before Spring MVC, so @RestControllerAdvice does not see their exceptions.
 * Handing the exception to the MVC exception resolver sends it through AuthExceptionHandler, and
 * the body is written by the same message converters as every other ProblemDetail of the API. The
 * standard bearer entry point runs first only to set the WWW-Authenticate header (RFC 6750).
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final BearerTokenAuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();

	private final HandlerExceptionResolver resolver;

	public ProblemDetailAuthenticationEntryPoint(HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		bearerEntryPoint.commence(request, response, authException);
		resolver.resolveException(request, response, null, authException);
	}

}
