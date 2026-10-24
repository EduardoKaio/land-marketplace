package io.github.eduardokaio.landmarketplace.shared.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	// Standard Spring errors can be 5xx too (503 here); those are not the client's fault
	@Test
	void logsServerSideSpringErrorsAsErrorWithStackTrace(CapturedOutput output) throws Exception {
		ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/land-lots"));

		ResponseEntity<Object> response = handler.handleException(new AsyncRequestTimeoutException(), request);

		assertThat(response.getStatusCode().value()).isEqualTo(503);
		assertThat(output).contains("ERROR")
			.contains("GET /api/land-lots failed")
			.contains("AsyncRequestTimeoutException")
			.contains("\tat ");
	}

}
