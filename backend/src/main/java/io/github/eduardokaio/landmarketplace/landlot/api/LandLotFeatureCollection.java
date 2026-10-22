package io.github.eduardokaio.landmarketplace.landlot.api;

import java.util.List;

/**
 * Land lots as a GeoJSON FeatureCollection (RFC 7946), returned by listing and search.
 */
public record LandLotFeatureCollection(String type, List<LandLotFeature> features) {

	private static final String TYPE = "FeatureCollection";

	public LandLotFeatureCollection(List<LandLotFeature> features) {
		this(TYPE, List.copyOf(features));
	}

}
