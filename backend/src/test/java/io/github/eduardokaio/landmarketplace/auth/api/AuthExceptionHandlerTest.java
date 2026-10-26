package io.github.eduardokaio.landmarketplace.auth.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionHandlerTest {

	@Test
	void accessDeniedBecomesAForbiddenProblemWithoutTheInternalMessage() {
		ProblemDetail problem = new AuthExceptionHandler()
			.handleAccessDenied(new AccessDeniedException("Access denied to secret thing"), new MockHttpServletRequest());

		assertThat(problem.getStatus()).isEqualTo(403);
		assertThat(problem.getType()).hasToString("/problems/forbidden");
		assertThat(problem.getDetail()).isEqualTo("You do not have permission to perform this action.");
	}

}
