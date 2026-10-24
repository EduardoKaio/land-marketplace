package io.github.eduardokaio.landmarketplace.landlot.application;

import java.math.BigDecimal;

import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.shared.geo.InvalidGeometryException;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.valid.TopologyValidationError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidGeometryRuleTest {

	private final ValidGeometryRule rule = new ValidGeometryRule();

	@Test
	void acceptsValidPolygon() {
		assertThatCode(() -> rule.validate(command(LandLotFixtures.pontaNegraSquare()))).doesNotThrowAnyException();
	}

	@Test
	void rejectsBowTieWithReasonAndLocation() {
		assertThatThrownBy(() -> rule.validate(command(LandLotFixtures.bowTie())))
			.isInstanceOf(InvalidGeometryException.class)
			.hasMessage("Self-intersection at (0.5, 0.5)");
	}

	@Test
	void rejectsHoleOutsideShell() {
		Polygon shell = LandLotFixtures.polygon(0, 0, 10, 0, 10, 10, 0, 10, 0, 0);
		Polygon outside = LandLotFixtures.polygon(20, 20, 20, 22, 22, 22, 22, 20, 20, 20);
		Polygon withBadHole = LandLotFixtures.GEOMETRY_FACTORY.createPolygon(shell.getExteriorRing(),
				new LinearRing[] { outside.getExteriorRing() });

		assertThatThrownBy(() -> rule.validate(command(withBadHole)))
			.isInstanceOf(InvalidGeometryException.class)
			.hasMessageStartingWith("Hole lies outside shell");
	}

	@Test
	void describesErrorWithoutLocationUsingOnlyTheReason() {
		TopologyValidationError error = new TopologyValidationError(TopologyValidationError.TOO_FEW_POINTS);

		assertThat(ValidGeometryRule.describe(error)).isEqualTo("Too few distinct points in geometry component");
	}

	private static CreateLandLotCommand command(Polygon geometry) {
		return new CreateLandLotCommand("Lot", "Desc", BigDecimal.TEN, LandLotFixtures.contact(), geometry);
	}

}
