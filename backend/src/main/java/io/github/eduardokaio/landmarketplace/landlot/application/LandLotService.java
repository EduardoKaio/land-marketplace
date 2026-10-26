package io.github.eduardokaio.landmarketplace.landlot.application;

import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotOverlapException;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotCreationLock;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import io.github.eduardokaio.landmarketplace.shared.geo.InvalidGeometryException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LandLotService {

	private final LandLotRepository repository;

	private final LandLotCreationLock creationLock;

	private final GeoJsonGeometryMapper geometryMapper;

	public LandLotService(LandLotRepository repository, LandLotCreationLock creationLock,
			GeoJsonGeometryMapper geometryMapper) {
		this.repository = repository;
		this.creationLock = creationLock;
		this.geometryMapper = geometryMapper;
	}

	@Transactional
	public LandLot create(CreateLandLotCommand command) {
		// First statement of the transaction, so the overlap check and the insert run under the lock
		creationLock.acquire();
		// Validity first: PostGIS spatial predicates fail on invalid geometries
		requireValidGeometry(command.geometry());
		requireNoOverlap(command.geometry());
		LandLot lot = LandLot.create(command.title(), command.description(), command.price(), command.contact(),
				command.geometry());
		// Flush inside the transaction so the generated area is read back and database errors surface here
		return repository.saveAndFlush(lot);
	}

	/** Rejects self-intersections and other invalid shapes, with the reason and location from JTS. */
	private static void requireValidGeometry(Polygon geometry) {
		IsValidOp validation = new IsValidOp(geometry);
		if (!validation.isValid()) {
			throw new InvalidGeometryException(describe(validation.getValidationError()));
		}
	}

	/** Rejects a polygon whose area overlaps the area of existing lots; the predicate runs in PostGIS. */
	private void requireNoOverlap(Polygon geometry) {
		List<UUID> conflictingIds = repository.findOverlappingIds(geometryMapper.toGeoJsonText(geometry));
		if (!conflictingIds.isEmpty()) {
			throw new LandLotOverlapException(conflictingIds);
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
