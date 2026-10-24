package io.github.eduardokaio.landmarketplace.landlot.domain;

import java.util.UUID;

/**
 * Raised when no land lot exists with the requested id.
 */
public class LandLotNotFoundException extends RuntimeException {

	public LandLotNotFoundException(UUID id) {
		super("No land lot with id " + id + ".");
	}

}
