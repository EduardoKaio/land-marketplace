package io.github.eduardokaio.landmarketplace.landlot.api;

import java.util.UUID;

import tools.jackson.databind.JsonNode;

/**
 * A land lot as a GeoJSON Feature (RFC 7946), readable directly by ol/format/GeoJSON.
 */
public record LandLotFeature(String type, UUID id, JsonNode geometry, LandLotProperties properties) {

	private static final String TYPE = "Feature";

	public LandLotFeature(UUID id, JsonNode geometry, LandLotProperties properties) {
		this(TYPE, id, geometry, properties);
	}

}
