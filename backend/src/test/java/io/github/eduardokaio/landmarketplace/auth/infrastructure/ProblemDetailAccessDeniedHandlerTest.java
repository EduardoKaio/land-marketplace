package io.github.eduardokaio.landmarketplace.auth.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * No current rule makes the filters deny an authenticated request (there are no roles), so the
 * hand-off to Spring MVC is checked directly; AuthExceptionHandlerTest covers the body.
 */
class ProblemDetailAccessDeniedHandlerTest {

	@Test
	void handsTheExceptionToTheMvcExceptionResolver() {
		HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AccessDeniedException denied = new AccessDeniedException("denied");

		new ProblemDetailAccessDeniedHandler(resolver).handle(request, response, denied);

		verify(resolver).resolveException(request, response, null, denied);
	}

}
