package io.github.eduardokaio.landmarketplace.landlot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Seller contact. A name and at least one channel (phone or email) are required.
 * Blank channels are stored as null so the database CHECK sees them as missing.
 */
@Embeddable
public record Contact(
		@Column(name = "contact_name", nullable = false, length = 120) String name,
		@Column(name = "contact_phone", length = 30) String phone,
		@Column(name = "contact_email", length = 254) String email) {

	public Contact {
		if (name == null || name.isBlank()) {
			throw new InvalidLandLotException("Contact name is required");
		}
		name = name.strip();
		phone = blankToNull(phone);
		email = blankToNull(email);
		if (phone == null && email == null) {
			throw new InvalidLandLotException("Contact phone or email is required");
		}
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}

}
