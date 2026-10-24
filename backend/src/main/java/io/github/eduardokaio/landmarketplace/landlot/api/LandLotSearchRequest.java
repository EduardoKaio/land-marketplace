package io.github.eduardokaio.landmarketplace.landlot.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Query parameters of the circle search. The upper radius limit is configurable, so it is checked
 * in LandLotQueryService instead of with a constant @Max here.
 *
 * @param lon center longitude in degrees
 * @param lat center latitude in degrees
 * @param radius radius in meters
 */
public record LandLotSearchRequest(@NotNull @DecimalMin("-180") @DecimalMax("180") Double lon,
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat, @NotNull @Positive Double radius) {
}
