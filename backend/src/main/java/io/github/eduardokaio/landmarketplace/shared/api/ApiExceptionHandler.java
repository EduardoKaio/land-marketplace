package io.github.eduardokaio.landmarketplace.shared.api;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import io.github.eduardokaio.landmarketplace.shared.geo.InvalidBoundingBoxException;
import io.github.eduardokaio.landmarketplace.shared.geo.InvalidGeometryException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps errors to RFC 9457 ProblemDetail responses. Extending ResponseEntityExceptionHandler gives
 * ProblemDetail bodies for the standard Spring MVC errors too (malformed JSON, wrong method...).
 * <p>
 * This is the fallback advice: feature advices run before it (see their @Order), and anything
 * nobody expected ends in {@link #handleUnexpected} as a 500 that reveals nothing internal.
 * <p>
 * Expected client errors are logged at WARN in one line, unexpected ones at ERROR with the stack
 * trace. Log lines carry the exception class, never its message: some messages echo client input,
 * which could forge log lines.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	/** One invalid request field, as listed in the "errors" property of a 400 response. */
	public record FieldError(String field, String message) {
	}

	/** Logs a request rejected with an expected 4xx error, without the stack trace. */
	public static void logRejected(HttpServletRequest request, HttpStatusCode status, Exception ex) {
		log.warn("{} {} rejected with {}: {}", request.getMethod(), request.getRequestURI(), status.value(),
				ex.getClass().getSimpleName());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldError> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
			.sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
			.toList();
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"The request has " + errors.size() + " invalid " + (errors.size() == 1 ? "field." : "fields."));
		problem.setType(URI.create("/problems/validation"));
		problem.setTitle("Invalid request");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	/** Every standard Spring MVC error goes through here, so this is where they are logged. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
		if (statusCode.is5xxServerError()) {
			logUnexpected(servletRequest, ex);
		}
		else {
			logRejected(servletRequest, statusCode, ex);
		}
		return super.handleExceptionInternal(ex, body, headers, statusCode, request);
	}

	/**
	 * 422: the body is well-formed JSON but the geometry cannot be accepted (wrong type, open ring,
	 * self-intersection...).
	 */
	@ExceptionHandler(InvalidGeometryException.class)
	ProblemDetail handleInvalidGeometry(InvalidGeometryException ex, HttpServletRequest request) {
		logRejected(request, HttpStatus.UNPROCESSABLE_CONTENT, ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
		problem.setType(URI.create("/problems/invalid-geometry"));
		problem.setTitle("Invalid geometry");
		return problem;
	}

	@ExceptionHandler(InvalidBoundingBoxException.class)
	ProblemDetail handleInvalidBoundingBox(InvalidBoundingBoxException ex, HttpServletRequest request) {
		logRejected(request, HttpStatus.BAD_REQUEST, ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setType(URI.create("/problems/invalid-bounding-box"));
		problem.setTitle("Invalid bounding box");
		return problem;
	}

	/**
	 * Anything no other handler expected. The detail is generic on purpose: the real message may
	 * contain SQL, class names or data, so it only goes to the log.
	 */
	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
		logUnexpected(request, ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred.");
		problem.setType(URI.create("/problems/internal-error"));
		problem.setTitle("Internal Server Error");
		return problem;
	}

	private static void logUnexpected(HttpServletRequest request, Exception ex) {
		log.error("{} {} failed", request.getMethod(), request.getRequestURI(), ex);
	}

}
