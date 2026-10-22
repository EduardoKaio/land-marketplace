package io.github.eduardokaio.landmarketplace.landlot.domain;

import java.math.BigDecimal;

import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import static io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures.contact;
import static io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures.pontaNegraSquare;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LandLotTest {

	private static final BigDecimal PRICE = new BigDecimal("250000.00");

	@Test
	void createsAvailableLotWithNormalizedValues() {
		LandLot lot = LandLot.create("  Corner lot  ", " Flat terrain ", new BigDecimal("250000"), contact(),
				pontaNegraSquare());

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
		LandLot lot = LandLot.create("Lot", "Desc", new BigDecimal("10.500"), contact(), pontaNegraSquare());

		assertThat(lot.getPrice()).hasToString("10.50");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void rejectsMissingTitle(String title) {
		assertThatThrownBy(() -> LandLot.create(title, "Desc", PRICE, contact(), pontaNegraSquare()))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Title is required");
	}

	@Test
	void rejectsTitleLongerThan120Characters() {
		String title = "x".repeat(121);

		assertThatThrownBy(() -> LandLot.create(title, "Desc", PRICE, contact(), pontaNegraSquare()))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Title must have at most 120 characters");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "   ")
	void rejectsMissingDescription(String description) {
		assertThatThrownBy(() -> LandLot.create("Lot", description, PRICE, contact(), pontaNegraSquare()))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Description is required");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "0.00", "-1" })
	void rejectsPriceNotGreaterThanZero(String price) {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", new BigDecimal(price), contact(), pontaNegraSquare()))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Price must be greater than zero");
	}

	@Test
	void rejectsNullPrice() {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", null, contact(), pontaNegraSquare()))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Price must be greater than zero");
	}

	@ParameterizedTest
	@ValueSource(strings = { "10.001", "12345678901234.00" })
	void rejectsPriceThatDoesNotFitNumeric15Comma2(String price) {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", new BigDecimal(price), contact(), pontaNegraSquare()))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Price must have at most 13 integer digits and 2 decimals");
	}

	@Test
	void rejectsMissingContact() {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", PRICE, null, pontaNegraSquare()))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Contact is required");
	}

	@Test
	void rejectsMissingGeometry() {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", PRICE, contact(), null))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Geometry is required");
	}

	@Test
	void rejectsGeometryInAnotherSrid() {
		Polygon webMercator = new GeometryFactory(new PrecisionModel(), 3857)
			.createPolygon(pontaNegraSquare().getCoordinates());

		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", PRICE, contact(), webMercator))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Geometry SRID must be 4326");
	}

	@Test
	void rejectsSelfIntersectingPolygon() {
		assertThatThrownBy(() -> LandLot.create("Lot", "Desc", PRICE, contact(), LandLotFixtures.bowTie()))
			.isInstanceOf(InvalidLandLotException.class)
			.hasMessage("Geometry must be a valid polygon without self-intersections");
	}

	@Test
	void pricePerSquareMeterNeedsTheAreaComputedByTheDatabase() {
		LandLot lot = LandLotFixtures.pontaNegraLot();

		assertThatThrownBy(lot::pricePerSquareMeter).isInstanceOf(IllegalStateException.class);
	}

}
