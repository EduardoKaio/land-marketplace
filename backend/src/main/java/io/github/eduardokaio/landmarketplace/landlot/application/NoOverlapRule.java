package io.github.eduardokaio.landmarketplace.landlot.application;

import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotOverlapException;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Rejects a polygon whose area overlaps the area of an existing lot. The spatial predicate runs in
 * PostGIS (see {@link LandLotRepository#findOverlappingIds(String)}).
 */
@Component
@Order(2)
public class NoOverlapRule implements LandLotRule {

	private final LandLotRepository repository;

	private final GeoJsonGeometryMapper geometryMapper;

	public NoOverlapRule(LandLotRepository repository, GeoJsonGeometryMapper geometryMapper) {
		this.repository = repository;
		this.geometryMapper = geometryMapper;
	}

	@Override
	public void validate(CreateLandLotCommand command) {
		List<UUID> conflictingIds = repository.findOverlappingIds(geometryMapper.toGeoJsonText(command.geometry()));
		if (!conflictingIds.isEmpty()) {
			throw new LandLotOverlapException(conflictingIds);
		}
	}

}
