package io.github.eduardokaio.landmarketplace.landlot.application;

import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.auth.domain.AccountNotFoundException;
import io.github.eduardokaio.landmarketplace.auth.infrastructure.AppUserRepository;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotAccessDeniedException;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotNotFoundException;
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

	private final AppUserRepository accounts;

	public LandLotService(LandLotRepository repository, LandLotCreationLock creationLock,
			GeoJsonGeometryMapper geometryMapper, AppUserRepository accounts) {
		this.repository = repository;
		this.creationLock = creationLock;
		this.geometryMapper = geometryMapper;
		this.accounts = accounts;
	}

	/** @param ownerId the account creating the lot, taken from its access token */
	@Transactional
	public LandLot create(CreateLandLotCommand command, UUID ownerId) {
		// First statement of the transaction, so the overlap check and the insert run under the lock
		creationLock.acquire();
		// A token stays valid after its account is gone (for example a reset database); without this
		// check the foreign key would fail on insert and the client would get a 500
		if (!accounts.existsById(ownerId)) {
			throw new AccountNotFoundException();
		}
		// Validity first: PostGIS spatial predicates fail on invalid geometries
		requireValidGeometry(command.geometry());
		requireNoOverlap(command.geometry());
		LandLot lot = LandLot.create(command.title(), command.description(), command.price(), command.contact(),
				command.geometry(), ownerId);
		// Flush inside the transaction so the generated area is read back and database errors surface here
		return repository.saveAndFlush(lot);
	}

	/** Replaces the details of a lot. No lock: the geometry does not change, so overlaps cannot appear. */
	@Transactional
	public LandLot update(UUID id, UpdateLandLotCommand command, UUID accountId) {
		LandLot lot = getOwned(id, accountId);
		lot.update(command.title(), command.description(), command.price(), command.contact(), command.status());
		// Flush so the new updated_at is written and constraint errors surface inside the transaction
		repository.flush();
		return lot;
	}

	@Transactional
	public void delete(UUID id, UUID accountId) {
		repository.delete(getOwned(id, accountId));
	}

	/**
	 * 404 before 403: telling a stranger that the lot exists reveals nothing, every lot is public.
	 */
	private LandLot getOwned(UUID id, UUID accountId) {
		LandLot lot = repository.findById(id).orElseThrow(() -> new LandLotNotFoundException(id));
		if (!lot.isOwnedBy(accountId)) {
			throw new LandLotAccessDeniedException(id);
		}
		return lot;
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
