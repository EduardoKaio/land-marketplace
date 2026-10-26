package io.github.eduardokaio.landmarketplace.auth.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Answers 403 raised by the security filters with a ProblemDetail, for the same reason as
 * {@link ProblemDetailAuthenticationEntryPoint}. Ownership checks ("only the owner edits a lot")
 * are business rules and answer 403 from the land lot feature instead.
 */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

	private final HandlerExceptionResolver resolver;

	public ProblemDetailAccessDeniedHandler(HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) {
		resolver.resolveException(request, response, null, accessDeniedException);
	}

}
