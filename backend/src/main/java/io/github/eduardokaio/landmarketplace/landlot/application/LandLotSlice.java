package io.github.eduardokaio.landmarketplace.landlot.application;

import java.util.List;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;

/**
 * A capped list of lots.
 *
 * @param truncated true when more lots matched than the cap allowed
 */
public record LandLotSlice(List<LandLot> lots, boolean truncated) {

	public LandLotSlice {
		lots = List.copyOf(lots);
	}

}
