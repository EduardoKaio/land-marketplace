package io.github.eduardokaio.landmarketplace.landlot.domain;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import static io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures.OWNER_ID;
import static io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures.contact;
import static io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures.pontaNegraSquare;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LandLotTest {

	private static final BigDecimal PRICE = new BigDecimal("250000.00");

	@Test
	void createsAvailableLotWithNormalizedValues() {
		LandLot lot = LandLot.create("  Corner lot  ", " Flat terrain ", new BigDecimal("250000"), contact(),
				pontaNegraSquare(), OWNER_ID);

		assertThat(lot.getTitle()).isEqualTo("Corner lot");
		assertThat(lot.getDescription()).isEqualTo("Flat terrain");
		assertThat(lot.getPrice()).isEqualByComparingTo("250000").hasToString("250000.00");
		assertThat(lot.getContact()).isEqualTo(contact());
		assertThat(lot.getGeom().equalsExact(pontaNegraSquare())).isTrue();
		assertThat(lot.getStatus()).isEqualTo(LandLotStatus.AVAILABLE);
		assertThat(lot.getId()).isNull();
		assertThat(lot.getAreaM2()).isNull();
		assertThat(lot.getCreatedAt()).isNull();
		assertThat(lot.getUpdatedAt()).isNull();
	}

	@Test
	void acceptsTrailingZerosBeyondTwoDecimals() {
		LandLot lot = LandLot.create("Lot", "Desc", new BigDecimal("10.500"), contact(), pontaNegraSquare(), OWNER_ID);

		assertThat(lot.getPrice()).hasToString("10.50");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void rejectsMissingTitle(String title) {
		assertThatThrownBy(() -> LandLot.create(title, "Desc", PRICE, contact(), pontaNegraSquare(), OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Title is required");
	}

	@Test
	void rejectsTitleLongerThan120Characters() {
		String title = "x".repeat(121);

		assertThatThrownBy(() -> LandLot.create(title, "Desc", PRICE, contact(), pontaNegraSquare(), OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Title must have at most 120 characters");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void rejectsMissingDescription(String description) {
		assertThatThrownBy(() -> LandLot.create("Lot", description, PRICE, contact(), pontaNegraSquare(), OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Description is required");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "0.00", "-1" })
	void rejectsPriceNotGreaterThanZero(String price) {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", new BigDecimal(price), contact(), pontaNegraSquare(), OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Price must be greater than zero");
	}

	@Test
	void rejectsNullPrice() {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", null, contact(), pontaNegraSquare(), OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Price must be greater than zero");
	}

	@ParameterizedTest
	@ValueSource(strings = { "10.001", "12345678901234.00" })
	void rejectsPriceThatDoesNotFitNumeric15Comma2(String price) {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", new BigDecimal(price), contact(), pontaNegraSquare(), OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Price must have at most 13 integer digits and 2 decimals");
	}

	@Test
	void rejectsMissingContact() {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", PRICE, null, pontaNegraSquare(), OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Contact is required");
	}

	@Test
	void rejectsMissingGeometry() {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", PRICE, contact(), null, OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Geometry is required");
	}

	@Test
	void rejectsGeometryInAnotherSrid() {
		Polygon webMercator = new GeometryFactory(new PrecisionModel(), 3857)
			.createPolygon(pontaNegraSquare().getCoordinates());

		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", PRICE, contact(), webMercator, OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Geometry SRID must be 4326");
	}

	@Test
	void rejectsSelfIntersectingPolygon() {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", PRICE, contact(), LandLotFixtures.bowTie(), OWNER_ID))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Geometry must be a valid polygon without self-intersections");
	}

	@Test
	void pricePerSquareMeterNeedsTheAreaComputedByTheDatabase() {
		LandLot lot = LandLotFixtures.pontaNegraLot();

		assertThatThrownBy(lot::pricePerSquareMeter).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void requiresAnOwnerForNewLots() {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", PRICE, contact(), pontaNegraSquare(), null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void belongsOnlyToItsOwner() {
		LandLot lot = LandLotFixtures.pontaNegraLot();

		assertThat(lot.isOwnedBy(OWNER_ID)).isTrue();
		assertThat(lot.isOwnedBy(UUID.randomUUID())).isFalse();
		assertThat(lot.isOwnedBy(null)).isFalse();
	}

	@Test
	void updatesTheDetailsAndKeepsTheGeometry() {
		LandLot lot = LandLotFixtures.pontaNegraLot();
		Contact newContact = new Contact("Ana", null, "ana@example.com");

		lot.update("  New title ", " New description ", new BigDecimal("300000"), newContact, LandLotStatus.SOLD);

		assertThat(lot.getTitle()).isEqualTo("New title");
		assertThat(lot.getDescription()).isEqualTo("New description");
		assertThat(lot.getPrice()).hasToString("300000.00");
		assertThat(lot.getContact()).isEqualTo(newContact);
		assertThat(lot.getStatus()).isEqualTo(LandLotStatus.SOLD);
		assertThat(lot.getGeom()).isEqualTo(pontaNegraSquare());
		assertThat(lot.getOwnerId()).isEqualTo(OWNER_ID);
	}

	@Test
	void updateAppliesTheSameRulesAsCreation() {
		LandLot lot = LandLotFixtures.pontaNegraLot();

		assertThatThrownBy(() -> lot.update(" ", "Desc", PRICE, contact(), LandLotStatus.SOLD))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Title is required");
		assertThatThrownBy(() -> lot.update("Lot", "Desc", PRICE, contact(), null))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Status is required");
	}

	@Test
	void rejectedUpdateChangesNothing() {
		LandLot lot = LandLotFixtures.pontaNegraLot();

		// The title is valid and comes first; the invalid description must not leave it half applied
		assertThatThrownBy(() -> lot.update("Changed title", " ", PRICE, contact(), LandLotStatus.SOLD))
			.isInstanceOf(InvalidLandLotException.class);

		assertThat(lot.getTitle()).isEqualTo("Corner lot near Ponta Negra");
		assertThat(lot.getStatus()).isEqualTo(LandLotStatus.AVAILABLE);
	}

}
