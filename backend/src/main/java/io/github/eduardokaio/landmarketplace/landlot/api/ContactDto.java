package io.github.eduardokaio.landmarketplace.landlot.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.eduardokaio.landmarketplace.landlot.domain.Contact;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactDto(
		@NotBlank @Size(max = 120) String name,
		@Size(max = 30) String phone,
		@Email @Size(max = 254) String email) {

	// Cross-field rule; @JsonIgnore keeps this validation-only getter out of the JSON
	@JsonIgnore
	@AssertTrue(message = "phone or email is required")
	public boolean isPhoneOrEmailPresent() {
		return hasText(phone) || hasText(email);
	}

	public Contact toDomain() {
		return new Contact(name, phone, email);
	}

	public static ContactDto from(Contact contact) {
		return new ContactDto(contact.name(), contact.phone(), contact.email());
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

}
