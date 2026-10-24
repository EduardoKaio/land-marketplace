package io.github.eduardokaio.landmarketplace.landlot.domain;

import java.util.List;
import java.util.UUID;

/**
 * Raised when a new land lot would overlap the area of existing lots.
 */
public class LandLotOverlapException extends RuntimeException {

	private final List<UUID> conflictingLotIds;

	public LandLotOverlapException(List<UUID> conflictingLotIds) {
		super(describe(conflictingLotIds.size()));
		this.conflictingLotIds = List.copyOf(conflictingLotIds);
	}

	public List<UUID> getConflictingLotIds() {
		return conflictingLotIds;
	}

	private static String describe(int count) {
		return "The submitted polygon overlaps " + count + " existing land " + (count == 1 ? "lot." : "lots.");
	}

}
