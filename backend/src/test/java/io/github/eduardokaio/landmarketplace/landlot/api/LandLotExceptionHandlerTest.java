package io.github.eduardokaio.landmarketplace.landlot.api;

import io.github.eduardokaio.landmarketplace.landlot.domain.InvalidLandLotException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LandLotExceptionHandlerTest {

	private final LandLotExceptionHandler handler = new LandLotExceptionHandler();

	// Bean Validation normally catches these first; the handler is the safety net for the domain checks
	@Test
	void mapsDomainInvariantViolationToBadRequest() {
		ProblemDetail problem = handler.handleInvalidLandLot(new InvalidLandLotException("Title is required"),
				new MockHttpServletRequest("POST", "/api/land-lots"));

		assertThat(problem.getStatus()).isEqualTo(400);
		assertThat(problem.getType()).hasToString("/problems/invalid-land-lot");
		assertThat(problem.getTitle()).isEqualTo("Invalid land lot");
		assertThat(problem.getDetail()).isEqualTo("Title is required");
	}

}
