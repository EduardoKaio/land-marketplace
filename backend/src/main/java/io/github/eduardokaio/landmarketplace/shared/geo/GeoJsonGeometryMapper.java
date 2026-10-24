package io.github.eduardokaio.landmarketplace.shared.geo;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converts the "geometry" member of a GeoJSON object to a JTS polygon and back.
 * <p>
 * The structure is checked before handing it to JTS because JTS reports problems with
 * generic exceptions (NullPointerException, IllegalArgumentException) that make poor API errors.
 * <p>
 * Coordinates are rounded to the precision grid of the geometry factory on the way in and written
 * with the same number of decimals on the way out.
 */
@Component
public class GeoJsonGeometryMapper {

	private static final String POLYGON = "Polygon";

	private static final int MIN_RING_POSITIONS = 4;

	private final GeometryFactory geometryFactory;

	private final JsonMapper jsonMapper = JsonMapper.shared();

	public GeoJsonGeometryMapper(GeometryFactory geometryFactory) {
		this.geometryFactory = geometryFactory;
	}

	public Polygon toPolygon(String geometryJson) {
		try {
			return toPolygon(jsonMapper.readTree(geometryJson));
		}
		catch (JacksonException ex) {
			throw new InvalidGeometryException("Malformed GeoJSON geometry", ex);
		}
	}

	public Polygon toPolygon(JsonNode geometry) {
		if (geometry == null || !geometry.isObject()) {
			throw new InvalidGeometryException("Geometry must be a GeoJSON object");
		}
		JsonNode type = geometry.get("type");
		if (type == null || !type.isString() || !POLYGON.equals(type.stringValue())) {
			throw new InvalidGeometryException("Geometry type must be Polygon, got: " + describe(type));
		}
		JsonNode rings = geometry.get("coordinates");
		if (rings == null || !rings.isArray() || rings.isEmpty()) {
			throw new InvalidGeometryException("Polygon coordinates are missing");
		}
		for (JsonNode ring : rings) {
			checkRing(ring);
		}
		return read(geometry);
	}

	public JsonNode toGeoJson(Polygon polygon) {
		return jsonMapper.readTree(toGeoJsonText(polygon));
	}

	/** GeoJSON text of the polygon, used for API responses and as input of native spatial queries. */
	public String toGeoJsonText(Polygon polygon) {
		GeoJsonWriter writer = new GeoJsonWriter(GeoConfig.COORDINATE_DECIMALS);
		// RFC 7946 removed the "crs" member; GeoJSON is always WGS84 lon/lat
		writer.setEncodeCRS(false);
		// RFC 7946 asks for counterclockwise exterior rings
		writer.setForceCCW(true);
		return writer.write(polygon);
	}

	private void checkRing(JsonNode ring) {
		if (!ring.isArray() || ring.size() < MIN_RING_POSITIONS) {
			throw new InvalidGeometryException(
					"Polygon ring must have at least " + MIN_RING_POSITIONS + " positions");
		}
		for (JsonNode position : ring) {
			if (!isPosition(position)) {
				throw new InvalidGeometryException("Polygon position must be [longitude, latitude]");
			}
		}
		JsonNode first = ring.get(0);
		JsonNode last = ring.get(ring.size() - 1);
		if (first.get(0).doubleValue() != last.get(0).doubleValue()
				|| first.get(1).doubleValue() != last.get(1).doubleValue()) {
			throw new InvalidGeometryException("Polygon ring is not closed");
		}
	}

	private static boolean isPosition(JsonNode position) {
		return position.isArray() && position.size() >= 2 && position.get(0).isNumber()
				&& position.get(1).isNumber();
	}

	private Polygon read(JsonNode geometry) {
		try {
			Geometry parsed = new GeoJsonReader(geometryFactory).read(geometry.toString());
			// Pointwise only rounds coordinates and never repairs topology; if rounding makes the
			// polygon invalid, the geometry validation rule reports it
			return (Polygon) GeometryPrecisionReducer.reducePointwise(parsed, geometryFactory.getPrecisionModel());
		}
		catch (ParseException | RuntimeException ex) {
			throw new InvalidGeometryException("Malformed GeoJSON geometry", ex);
		}
	}

	private static String describe(JsonNode type) {
		return (type == null || type.isNull()) ? "nothing" : type.toString();
	}

}
