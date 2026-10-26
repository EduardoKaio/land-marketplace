package io.github.eduardokaio.landmarketplace.landlot.api;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class CreateLandLotRequestValidationTest {

	private static final JsonNode GEOMETRY = JsonMapper.shared().readTree("""
			{"type": "Polygon", "coordinates": [[[0, 0], [1, 0], [1, 1], [0, 0]]]}""");

	private static ValidatorFactory validatorFactory;

	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void acceptsContractExample() {
		assertThat(validator.validate(request(new BigDecimal("250000.00"), contact("(84) 99999-0000", "kaio@example.com"))))
			.isEmpty();
	}

	@Test
	void acceptsContactWithOnlyOneChannel() {
		assertThat(validator.validate(request(BigDecimal.TEN, contact(null, "kaio@example.com")))).isEmpty();
		assertThat(validator.validate(request(BigDecimal.TEN, contact("(84) 99999-0000", null)))).isEmpty();
	}

	// A blank phone means "no phone", like null
	@Test
	void acceptsBlankPhoneWhenThereIsAnEmail() {
		assertThat(validator.validate(request(BigDecimal.TEN, contact(" ", "kaio@example.com")))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "(84) 3333-4444", "(84) 99999-8888", "(11) 2345-6789", "(99) 91234-5678" })
	void acceptsBrazilianLandlinesAndMobiles(String phone) {
		assertThat(validator.validate(request(BigDecimal.TEN, contact(phone, null)))).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			// Not in the masked format
			"84999998888", "+55 84 99999-8888", "84 99999-8888",
			// No DDD has a zero
			"(04) 99999-8888", "(80) 3333-4444",
			// A landline starts with 2 to 5, a mobile has 9 digits and starts with 9
			"(84) 1333-4444", "(84) 6333-4444", "(84) 89999-8888", "(84) 9999-8888",
			// Too many digits
			"(84) 99999-88888" })
	void rejectsPhonesOutsideTheBrazilianFormat(String phone) {
		assertThat(violatedFields(request(BigDecimal.TEN, contact(phone, "kaio@example.com"))))
			.containsExactly("contact.phone");
	}

	@ParameterizedTest
	@ValueSource(strings = { "not-an-email", "ana@localhost", "ana@example.c" })
	void rejectsEmailsWithoutAFullDomain(String email) {
		assertThat(violatedFields(request(BigDecimal.TEN, contact("(84) 99999-0000", email))))
			.containsExactly("contact.email");
	}

	@Test
	void rejectsMissingAndInvalidFields() {
		CreateLandLotRequest request = new CreateLandLotRequest(" ", new BigDecimal("-1.001"), "", null, null);

		assertThat(violatedFields(request)).containsExactlyInAnyOrder("title", "price", "description", "contact",
				"geometry");
	}

	@Test
	void rejectsContactWithoutPhoneAndEmailAndInvalidEmail() {
		assertThat(violatedFields(request(BigDecimal.TEN, contact(" ", null))))
			.containsExactly("contact.phoneOrEmailPresent");
		assertThat(violatedFields(request(BigDecimal.TEN, contact(null, "not-an-email"))))
			.containsExactly("contact.email");
	}

	@Test
	void validationOnlyGetterIsNotPartOfTheJson() {
		String json = JsonMapper.shared().writeValueAsString(contact("(84) 99999-0000", null));

		assertThat(json).doesNotContain("phoneOrEmailPresent");
	}

	private Set<String> violatedFields(CreateLandLotRequest request) {
		return validator.validate(request)
			.stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(Object::toString)
			.collect(java.util.stream.Collectors.toSet());
	}

	private static CreateLandLotRequest request(BigDecimal price, ContactDto contact) {
		return new CreateLandLotRequest("Corner lot", price, "Flat terrain", contact, GEOMETRY);
	}

	private static ContactDto contact(String phone, String email) {
		return new ContactDto("Kaio Eduardo", phone, email);
	}

}
