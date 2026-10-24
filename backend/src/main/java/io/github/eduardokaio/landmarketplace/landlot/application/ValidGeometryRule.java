package io.github.eduardokaio.landmarketplace.landlot.application;

import io.github.eduardokaio.landmarketplace.shared.geo.InvalidGeometryException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Rejects topologically invalid polygons (self-intersections, holes outside the shell...) with
 * the reason and location reported by JTS. Runs first: PostGIS spatial predicates fail on invalid
 * geometries, so the overlap rule must never receive one.
 */
@Component
@Order(1)
public class ValidGeometryRule implements LandLotRule {

	@Override
	public void validate(CreateLandLotCommand command) {
		IsValidOp validation = new IsValidOp(command.geometry());
		if (!validation.isValid()) {
			throw new InvalidGeometryException(describe(validation.getValidationError()));
		}
	}

	// Package-private for the test: JTS errors normally carry a location, but the API allows none,
	// and a NullPointerException here would turn a client error into a 500
	static String describe(TopologyValidationError error) {
		Coordinate location = error.getCoordinate();
		if (location == null) {
			return error.getMessage();
		}
		return error.getMessage() + " at (" + location.getX() + ", " + location.getY() + ")";
	}

}
