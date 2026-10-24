package io.github.eduardokaio.landmarketplace.shared.api;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import io.github.eduardokaio.landmarketplace.shared.geo.InvalidBoundingBoxException;
import io.github.eduardokaio.landmarketplace.shared.geo.InvalidGeometryException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps errors to RFC 9457 ProblemDetail responses. Extending ResponseEntityExceptionHandler gives
 * ProblemDetail bodies for the standard Spring MVC errors too (malformed JSON, wrong method...).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	/** One invalid request field, as listed in the "errors" property of a 400 response. */
	public record FieldError(String field, String message) {
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

	/**
	 * 422: the body is well-formed JSON but the geometry cannot be accepted (wrong type, open ring,
	 * self-intersection...).
	 */
	@ExceptionHandler(InvalidGeometryException.class)
	ProblemDetail handleInvalidGeometry(InvalidGeometryException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
		problem.setType(URI.create("/problems/invalid-geometry"));
		problem.setTitle("Invalid geometry");
		return problem;
	}

	@ExceptionHandler(InvalidBoundingBoxException.class)
	ProblemDetail handleInvalidBoundingBox(InvalidBoundingBoxException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setType(URI.create("/problems/invalid-bounding-box"));
		problem.setTitle("Invalid bounding box");
		return problem;
	}

}
