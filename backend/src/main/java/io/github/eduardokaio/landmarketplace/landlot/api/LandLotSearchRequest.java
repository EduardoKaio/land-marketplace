package io.github.eduardokaio.landmarketplace.landlot.api;

import io.swagger.v3.oas.annotations.media.Schema;
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
public record LandLotSearchRequest(
		@Schema(description = "Center longitude in degrees", example = "-35.1955") @NotNull @DecimalMin("-180")
		@DecimalMax("180") Double lon,
		@Schema(description = "Center latitude in degrees", example = "-5.8795") @NotNull @DecimalMin("-90")
		@DecimalMax("90") Double lat,
		@Schema(description = "Radius in meters, up to the configured maximum (50000 by default)",
				example = "1500") @NotNull @Positive Double radius) {
}
