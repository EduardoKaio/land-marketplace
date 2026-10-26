package io.github.eduardokaio.landmarketplace.landlot.api;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotFilter;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Optional filter query parameters of the map listing and of the circle search. Every bound is
 * inclusive; a missing parameter does not filter.
 */
public record LandLotFilterRequest(
		@Schema(description = "Lowest total price", example = "100000") @PositiveOrZero BigDecimal minPrice,
		@Schema(description = "Highest total price", example = "500000") @PositiveOrZero BigDecimal maxPrice,
		@Schema(description = "Smallest area in square meters", example = "300") @PositiveOrZero Double minArea,
		@Schema(description = "Largest area in square meters", example = "20000") @PositiveOrZero Double maxArea,
		@Schema(description = "Highest price per square meter, compared with the value rounded to cents",
				example = "50") @PositiveOrZero BigDecimal maxPricePerSquareMeter,
		@Schema(description = "Only lots with this status") LandLotStatus status) {

	// Cross-field rules, like ContactDto; @JsonIgnore and hidden keep these validation-only
	// getters out of the JSON and of the OpenAPI parameters
	@JsonIgnore
	@Schema(hidden = true)
	@AssertTrue(message = "minPrice must not be greater than maxPrice")
	public boolean isPriceRangeOrdered() {
		return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
	}

	@JsonIgnore
	@Schema(hidden = true)
	@AssertTrue(message = "minArea must not be greater than maxArea")
	public boolean isAreaRangeOrdered() {
		return minArea == null || maxArea == null || minArea <= maxArea;
	}

	public LandLotFilter toFilter() {
		return new LandLotFilter(minPrice, maxPrice, minArea, maxArea, maxPricePerSquareMeter, status);
	}

}
