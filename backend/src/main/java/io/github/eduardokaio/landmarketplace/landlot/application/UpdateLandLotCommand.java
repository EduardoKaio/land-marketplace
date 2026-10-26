package io.github.eduardokaio.landmarketplace.landlot.application;

import java.math.BigDecimal;

import io.github.eduardokaio.landmarketplace.landlot.domain.Contact;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotStatus;

/**
 * Input of the land lot update use case: every editable detail. The geometry is not among them.
 */
public record UpdateLandLotCommand(String title, String description, BigDecimal price, Contact contact,
		LandLotStatus status) {

}
