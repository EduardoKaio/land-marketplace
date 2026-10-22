package io.github.eduardokaio.landmarketplace.landlot.domain;

/**
 * Raised when data used to build a land lot breaks one of its invariants.
 */
public class InvalidLandLotException extends RuntimeException {

	public InvalidLandLotException(String message) {
		super(message);
	}

}
