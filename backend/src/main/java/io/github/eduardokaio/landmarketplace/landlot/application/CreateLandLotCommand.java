package io.github.eduardokaio.landmarketplace.landlot.application;

import java.math.BigDecimal;

import io.github.eduardokaio.landmarketplace.landlot.domain.Contact;
import org.locationtech.jts.geom.Polygon;

/**
 * Input of the land lot creation use case, already converted from HTTP DTOs to domain types.
 */
public record CreateLandLotCommand(String title, String description, BigDecimal price, Contact contact,
		Polygon geometry) {

}
