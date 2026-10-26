package io.github.eduardokaio.landmarketplace.landlot.domain;

import java.util.UUID;

/**
 * Raised when someone other than the owner tries to change or delete a lot.
 */
public class LandLotAccessDeniedException extends RuntimeException {

	public LandLotAccessDeniedException(UUID id) {
		super("Only the owner can change or delete land lot " + id + ".");
	}

}
