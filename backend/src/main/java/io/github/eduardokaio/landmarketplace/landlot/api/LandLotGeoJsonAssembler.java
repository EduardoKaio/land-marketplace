package io.github.eduardokaio.landmarketplace.landlot.api;

import java.util.List;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import org.springframework.stereotype.Component;

/**
 * Builds the GeoJSON responses from persisted land lots, keeping JSON concerns out of the entity.
 */
@Component
public class LandLotGeoJsonAssembler {

	private final GeoJsonGeometryMapper geometryMapper;

	public LandLotGeoJsonAssembler(GeoJsonGeometryMapper geometryMapper) {
		this.geometryMapper = geometryMapper;
	}

	public LandLotFeature toFeature(LandLot lot) {
		LandLotProperties properties = new LandLotProperties(lot.getTitle(), lot.getPrice(),
				roundToTwoDecimals(lot.getAreaM2()), lot.pricePerSquareMeter(), lot.getDescription(),
				ContactDto.from(lot.getContact()), lot.getStatus(), lot.getCreatedAt());
		return new LandLotFeature(lot.getId(), geometryMapper.toGeoJson(lot.getGeom()), properties);
	}

	public LandLotFeatureCollection toCollection(List<LandLot> lots) {
		return toCollection(lots, false);
	}

	public LandLotFeatureCollection toCollection(List<LandLot> lots, boolean truncated) {
		return new LandLotFeatureCollection(lots.stream().map(this::toFeature).toList(), truncated);
	}

	// More decimals would only be noise: a hand drawn polygon is not that precise
	private static double roundToTwoDecimals(double areaSquareMeters) {
		return Math.round(areaSquareMeters * 100) / 100.0;
	}

}
