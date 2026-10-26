package io.github.eduardokaio.landmarketplace.landlot;

import java.math.BigDecimal;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.domain.Contact;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoConfig;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

/**
 * Test data based on the example of the API contract (a lot near Ponta Negra, Natal).
 */
public final class LandLotFixtures {

	public static final GeometryFactory GEOMETRY_FACTORY = GeoConfig.newGeometryFactory();

	/** Owner of lots in tests that never reach the database, where no account has to exist. */
	public static final UUID OWNER_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");

	/** Geodesic area of {@link #pontaNegraSquare()} as given in the API contract example. */
	public static final double PONTA_NEGRA_AREA_M2 = 12247.3;

	private LandLotFixtures() {
	}

	public static Polygon pontaNegraSquare() {
		return polygon(-35.1960, -5.8800, -35.1950, -5.8800, -35.1950, -5.8790, -35.1960, -5.8790, -35.1960, -5.8800);
	}

	/** Bow tie: the ring crosses itself, so the polygon is invalid. */
	public static Polygon bowTie() {
		return polygon(0, 0, 1, 1, 1, 0, 0, 1, 0, 0);
	}

	public static Contact contact() {
		return new Contact("Kaio Eduardo", "(84) 99999-0000", "kaio@example.com");
	}

	public static LandLot pontaNegraLot() {
		return pontaNegraLot(OWNER_ID);
	}

	public static LandLot pontaNegraLot(UUID ownerId) {
		return LandLot.create("Corner lot near Ponta Negra", "Flat terrain, ready to build, two blocks from the beach.",
				new BigDecimal("250000.00"), contact(), pontaNegraSquare(), ownerId);
	}

	public static Polygon polygon(double... lonLatPairs) {
		Coordinate[] coordinates = new Coordinate[lonLatPairs.length / 2];
		for (int i = 0; i < coordinates.length; i++) {
			coordinates[i] = new Coordinate(lonLatPairs[2 * i], lonLatPairs[2 * i + 1]);
		}
		return GEOMETRY_FACTORY.createPolygon(coordinates);
	}

}
