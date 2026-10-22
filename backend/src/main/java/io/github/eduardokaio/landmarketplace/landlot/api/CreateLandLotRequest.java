package io.github.eduardokaio.landmarketplace.landlot.api;

import java.math.BigDecimal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * Body of POST /api/land-lots. The geometry stays as raw JSON here and is validated and converted
 * to JTS by GeoJsonGeometryMapper, which gives specific errors for each GeoJSON problem.
 */
public record CreateLandLotRequest(
		@NotBlank @Size(max = 120) String title,
		@NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal price,
		@NotBlank String description,
		@NotNull @Valid ContactDto contact,
		@NotNull JsonNode geometry) {

}
