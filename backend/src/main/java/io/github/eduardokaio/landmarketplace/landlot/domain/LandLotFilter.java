package io.github.eduardokaio.landmarketplace.landlot.domain;

import java.math.BigDecimal;

/**
 * Optional criteria of the map listing and of the circle search. A null field does not filter.
 * Bounds are inclusive.
 *
 * @param maxPricePerSquareMeter compared with the price per m² rounded to cents, as the API shows it
 */
public record LandLotFilter(BigDecimal minPrice, BigDecimal maxPrice, Double minArea, Double maxArea,
		BigDecimal maxPricePerSquareMeter, LandLotStatus status) {

	public static final LandLotFilter NONE = new LandLotFilter(null, null, null, null, null, null);

}
