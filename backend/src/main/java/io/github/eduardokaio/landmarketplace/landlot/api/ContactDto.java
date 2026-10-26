package io.github.eduardokaio.landmarketplace.landlot.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.eduardokaio.landmarketplace.landlot.domain.Contact;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import io.github.eduardokaio.landmarketplace.shared.api.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Phone and email formats are checked only here, on input: lots stored before these rules existed
 * keep their contact as it was, and the domain still loads them.
 */
public record ContactDto(
		@NotBlank @Size(max = 120) String name,
		// Blank is allowed, like null: it means "no phone" (Contact turns it into null), and
		// isPhoneOrEmailPresent reports it when the email is missing too
		@Pattern(regexp = "\\s*|" + ValidationPatterns.BRAZILIAN_PHONE,
				message = ValidationPatterns.BRAZILIAN_PHONE_MESSAGE) String phone,
		@Email(regexp = ValidationPatterns.EMAIL_WITH_DOMAIN) @Size(max = 254) String email) {

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
