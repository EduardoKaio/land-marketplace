package io.github.eduardokaio.landmarketplace.shared.geo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Polygon;
import tools.jackson.databind.JsonNode;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoJsonGeometryMapperTest {

	private static final String SQUARE = """
			{"type": "Polygon", "coordinates": [[
			  [-35.1960, -5.8800], [-35.1950, -5.8800],
			  [-35.1950, -5.8790], [-35.1960, -5.8790],
			  [-35.1960, -5.8800]
			]]}""";

	private static final String SQUARE_WITH_HOLE = """
			{"type": "Polygon", "coordinates": [
			  [[0, 0], [10, 0], [10, 10], [0, 10], [0, 0]],
			  [[2, 2], [2, 4], [4, 4], [4, 2], [2, 2]]
			]}""";

	private final GeoJsonGeometryMapper mapper = new GeoJsonGeometryMapper(GeoConfig.newGeometryFactory());

	@Test
	void convertsPolygonWithLongitudeAsXAndLatitudeAsY() {
		Polygon polygon = mapper.toPolygon(SQUARE);

		assertThat(polygon.getSRID()).isEqualTo(GeoConfig.SRID);
		assertThat(polygon.getExteriorRing().getNumPoints()).isEqualTo(5);
		assertThat(polygon.getCoordinates()[0].getX()).isEqualTo(-35.1960);
		assertThat(polygon.getCoordinates()[0].getY()).isEqualTo(-5.8800);
	}

	@Test
	void convertsPolygonWithHole() {
		Polygon polygon = mapper.toPolygon(SQUARE_WITH_HOLE);

		assertThat(polygon.getNumInteriorRing()).isEqualTo(1);
		assertThat(polygon.getArea()).isEqualTo(100 - 4);
	}

	@Test
	void writesGeoJsonWithoutCrsThatReadsBackToTheSamePolygon() {
		Polygon polygon = mapper.toPolygon(SQUARE);

		JsonNode geoJson = mapper.toGeoJson(polygon);

		assertThat(geoJson.get("type").stringValue()).isEqualTo("Polygon");
		assertThat(geoJson.has("crs")).isFalse();
		assertThat(mapper.toPolygon(geoJson).equalsTopo(polygon)).isTrue();
	}

	@Test
	void roundsIncomingCoordinatesToSevenDecimals() {
		Polygon polygon = mapper.toPolygon("""
				{"type": "Polygon", "coordinates": [[
				  [-35.19501234567, -5.88000000004], [-35.1940, -5.8800],
				  [-35.1940, -5.8790], [-35.19501234567, -5.88000000004]
				]]}""");

		assertThat(polygon.getCoordinates()[0].getX()).isEqualTo(-35.1950123);
		assertThat(polygon.getCoordinates()[0].getY()).isEqualTo(-5.88);
		assertThat(polygon.getSRID()).isEqualTo(GeoConfig.SRID);
	}

	@Test
	void writesSevenDecimalsSoTheTextReadsBackToTheSameCoordinates() {
		Polygon polygon = mapper.toPolygon("""
				{"type": "Polygon", "coordinates": [[
				  [-35.1950123, -5.8800001], [-35.1940007, -5.8800001],
				  [-35.1940007, -5.8790009], [-35.1950123, -5.8800001]
				]]}""");

		String text = mapper.toGeoJsonText(polygon);

		assertThat(text).contains("-35.1950123").contains("-5.8790009").doesNotContain("crs");
		assertThat(mapper.toPolygon(text).equalsExact(polygon)).isTrue();
	}

	@Test
	void writesExteriorRingCounterclockwiseAsRfc7946Requires() {
		Polygon clockwise = mapper.toPolygon("""
				{"type": "Polygon", "coordinates": [[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]]}""");

		JsonNode secondPosition = mapper.toGeoJson(clockwise).get("coordinates").get(0).get(1);

		assertThat(secondPosition.get(0).doubleValue()).isEqualTo(1.0);
		assertThat(secondPosition.get(1).doubleValue()).isEqualTo(0.0);
	}

	static Stream<Arguments> invalidGeometries() {
		return Stream.of(
				Arguments.of("not an object", "[1, 2]", "Geometry must be a GeoJSON object"),
				Arguments.of("Point", """
						{"type": "Point", "coordinates": [1, 2]}""", "Geometry type must be Polygon, got: \"Point\""),
				Arguments.of("MultiPolygon", """
						{"type": "MultiPolygon", "coordinates": []}""", "Geometry type must be Polygon"),
				Arguments.of("type missing", """
						{"coordinates": [[[0, 0], [1, 0], [1, 1], [0, 0]]]}""", "Geometry type must be Polygon, got: nothing"),
				Arguments.of("coordinates missing", """
						{"type": "Polygon"}""", "Polygon coordinates are missing"),
				Arguments.of("coordinates empty", """
						{"type": "Polygon", "coordinates": []}""", "Polygon coordinates are missing"),
				Arguments.of("coordinates not an array", """
						{"type": "Polygon", "coordinates": "x"}""", "Polygon coordinates are missing"),
				Arguments.of("ring too short", """
						{"type": "Polygon", "coordinates": [[[0, 0], [1, 0], [0, 0]]]}""",
						"Polygon ring must have at least 4 positions"),
				Arguments.of("ring not an array", """
						{"type": "Polygon", "coordinates": [1]}""", "Polygon ring must have at least 4 positions"),
				Arguments.of("position not numeric", """
						{"type": "Polygon", "coordinates": [[[0, 0], [1, "a"], [1, 1], [0, 0]]]}""",
						"Polygon position must be [longitude, latitude]"),
				Arguments.of("position with one value", """
						{"type": "Polygon", "coordinates": [[[0, 0], [1], [1, 1], [0, 0]]]}""",
						"Polygon position must be [longitude, latitude]"),
				Arguments.of("longitude not closed", """
						{"type": "Polygon", "coordinates": [[[0, 0], [1, 0], [1, 1], [0, 1], [0.5, 0]]]}""",
						"Polygon ring is not closed"),
				Arguments.of("hole not closed", """
						{"type": "Polygon", "coordinates": [
						  [[0, 0], [10, 0], [10, 10], [0, 10], [0, 0]],
						  [[2, 2], [2, 4], [4, 4], [4, 2], [2, 3]]
						]}""", "Polygon ring is not closed"),
				Arguments.of("latitude not closed", """
						{"type": "Polygon", "coordinates": [[[0, 0], [1, 0], [1, 1], [0, 1], [0, 0.5]]]}""",
						"Polygon ring is not closed"),
				Arguments.of("malformed JSON", "{\"type\": \"Polygon\", ", "Malformed GeoJSON geometry"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidGeometries")
	void rejectsInvalidGeometryWithClearMessage(String scenario, String json, String expectedMessage) {
		assertThatThrownBy(() -> mapper.toPolygon(json))
			.isInstanceOf(InvalidGeometryException.class)
			.hasMessageStartingWith(expectedMessage);
	}

	@Test
	void rejectsNullGeometry() {
		assertThatThrownBy(() -> mapper.toPolygon((JsonNode) null))
			.isInstanceOf(InvalidGeometryException.class)
			.hasMessage("Geometry must be a GeoJSON object");
	}

}
