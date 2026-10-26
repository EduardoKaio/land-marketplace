package io.github.eduardokaio.landmarketplace.landlot.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContactTest {

	@Test
	void stripsValuesAndTurnsBlankChannelsIntoNull() {
		Contact contact = new Contact(" Kaio ", "  ", " kaio@example.com ");

		assertThat(contact.name()).isEqualTo("Kaio");
		assertThat(contact.phone()).isNull();
		assertThat(contact.email()).isEqualTo("kaio@example.com");
	}

	@Test
	void acceptsPhoneWithoutEmail() {
		Contact contact = new Contact("Kaio", "(84) 99999-0000", null);

		assertThat(contact.phone()).isEqualTo("(84) 99999-0000");
		assertThat(contact.email()).isNull();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "  ")
	void rejectsMissingName(String name) {
		assertThatThrownBy(() -> new Contact(name, "123", null))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Contact name is required");
	}

	@Test
	void rejectsContactWithoutPhoneAndEmail() {
		assertThatThrownBy(() -> new Contact("Kaio", " ", null))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Contact phone or email is required");
	}

}
