package io.github.eduardokaio.landmarketplace.landlot.api;

import java.math.BigDecimal;
import java.time.Instant;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotStatus;

/**
 * GeoJSON Feature "properties". Component order matches the JSON order of the API contract.
 */
public record LandLotProperties(
		String title,
		BigDecimal price,
		double areaSquareMeters,
		BigDecimal pricePerSquareMeter,
		String description,
		ContactDto contact,
		LandLotStatus status,
		Instant createdAt) {

}
