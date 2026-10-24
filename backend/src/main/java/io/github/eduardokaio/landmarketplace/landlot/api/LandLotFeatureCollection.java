package io.github.eduardokaio.landmarketplace.landlot.api;

import java.util.List;

/**
 * Land lots as a GeoJSON FeatureCollection (RFC 7946), returned by listing and search.
 * <p>
 * "truncated" is a foreign member, which RFC 7946 allows: it tells the client that more lots
 * matched than were returned, so the map can ask the user to zoom in.
 */
public record LandLotFeatureCollection(String type, List<LandLotFeature> features, boolean truncated) {

	private static final String TYPE = "FeatureCollection";

	public LandLotFeatureCollection(List<LandLotFeature> features, boolean truncated) {
		this(TYPE, List.copyOf(features), truncated);
	}

}
