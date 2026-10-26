package io.github.eduardokaio.landmarketplace.landlot.api;

import java.math.BigDecimal;

import io.github.eduardokaio.landmarketplace.landlot.application.UpdateLandLotCommand;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/land-lots/{id}: every editable detail, all required, as PUT replaces them. No
 * geometry: the shape of a lot cannot change after it is published.
 */
public record UpdateLandLotRequest(
		@NotBlank @Size(max = 120) String title,
		@NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal price,
		@NotBlank String description,
		@NotNull @Valid ContactDto contact,
		@NotNull LandLotStatus status) {

	public UpdateLandLotCommand toCommand() {
		return new UpdateLandLotCommand(title, description, price, contact.toDomain(), status);
	}

}
