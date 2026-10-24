package io.github.eduardokaio.landmarketplace.landlot.api;

import java.net.URI;

import io.github.eduardokaio.landmarketplace.landlot.domain.InvalidLandLotException;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotNotFoundException;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotOverlapException;
import io.github.eduardokaio.landmarketplace.landlot.domain.SearchRadiusTooLargeException;
import io.github.eduardokaio.landmarketplace.shared.api.ApiExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Land lot specific errors. Kept in the feature so the shared handler does not depend on it.
 * <p>
 * Spring asks the advices in order and uses the first one that has any matching handler. The
 * shared advice has a catch-all for Exception, so this one must come first or the shared
 * catch-all would turn these errors into 500. Without @Order the order would depend on bean
 * registration order, which today happens to follow package names.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class LandLotExceptionHandler {

	@ExceptionHandler(LandLotOverlapException.class)
	ProblemDetail handleOverlap(LandLotOverlapException ex, HttpServletRequest request) {
		ApiExceptionHandler.logRejected(request, HttpStatus.CONFLICT, ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setType(URI.create("/problems/land-lot-overlap"));
		problem.setTitle("Land lot overlaps existing lots");
		// Lets the frontend highlight the conflicting lots on the map
		problem.setProperty("conflictingLotIds", ex.getConflictingLotIds());
		return problem;
	}

	/** Domain invariants that Bean Validation did not catch first. */
	@ExceptionHandler(InvalidLandLotException.class)
	ProblemDetail handleInvalidLandLot(InvalidLandLotException ex, HttpServletRequest request) {
		ApiExceptionHandler.logRejected(request, HttpStatus.BAD_REQUEST, ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setType(URI.create("/problems/invalid-land-lot"));
		problem.setTitle("Invalid land lot");
		return problem;
	}

	@ExceptionHandler(LandLotNotFoundException.class)
	ProblemDetail handleNotFound(LandLotNotFoundException ex, HttpServletRequest request) {
		ApiExceptionHandler.logRejected(request, HttpStatus.NOT_FOUND, ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setType(URI.create("/problems/land-lot-not-found"));
		problem.setTitle("Land lot not found");
		return problem;
	}

	@ExceptionHandler(SearchRadiusTooLargeException.class)
	ProblemDetail handleSearchRadiusTooLarge(SearchRadiusTooLargeException ex, HttpServletRequest request) {
		ApiExceptionHandler.logRejected(request, HttpStatus.BAD_REQUEST, ex);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setType(URI.create("/problems/search-radius-too-large"));
		problem.setTitle("Search radius too large");
		// Lets the frontend cap the circle while the user drags it
		problem.setProperty("maxRadiusMeters", ex.getMaxRadiusMeters());
		return problem;
	}

}
