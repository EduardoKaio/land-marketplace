package io.github.eduardokaio.landmarketplace.landlot.api;

import java.net.URI;

import io.github.eduardokaio.landmarketplace.landlot.domain.InvalidLandLotException;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotOverlapException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Land lot specific errors. Kept in the feature so the shared handler does not depend on it.
 */
@RestControllerAdvice
public class LandLotExceptionHandler {

	@ExceptionHandler(LandLotOverlapException.class)
	ProblemDetail handleOverlap(LandLotOverlapException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problem.setType(URI.create("/problems/land-lot-overlap"));
		problem.setTitle("Land lot overlaps existing lots");
		// Lets the frontend highlight the conflicting lots on the map
		problem.setProperty("conflictingLotIds", ex.getConflictingLotIds());
		return problem;
	}

	/** Domain invariants that Bean Validation did not catch first. */
	@ExceptionHandler(InvalidLandLotException.class)
	ProblemDetail handleInvalidLandLot(InvalidLandLotException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problem.setType(URI.create("/problems/invalid-land-lot"));
		problem.setTitle("Invalid land lot");
		return problem;
	}

}
