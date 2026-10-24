package io.github.eduardokaio.landmarketplace.shared.geo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundingBoxTest {

	@Test
	void parsesFourNumbersToleratingSpaces() {
		assertThat(BoundingBox.parse(" -35.21, -5.91 ,-35.19,-5.89 "))
			.isEqualTo(new BoundingBox(-35.21, -5.91, -35.19, -5.89));
	}

	@Test
	void acceptsTheWholeWorld() {
		assertThat(BoundingBox.parse("-180,-90,180,90")).isEqualTo(new BoundingBox(-180, -90, 180, 90));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "1,2,3", "1,2,3,4,5", "1,2,3,", "a,2,3,4", "NaN,2,3,4", "1,Infinity,3,4" })
	void rejectsAnythingButFourFiniteNumbers(String text) {
		assertThatThrownBy(() -> BoundingBox.parse(text)).isInstanceOf(InvalidBoundingBoxException.class)
			.hasMessage("The bounding box must be minLon,minLat,maxLon,maxLat");
	}

	@ParameterizedTest
	@ValueSource(strings = { "-180.1,0,1,1", "0,0,180.1,1" })
	void rejectsLongitudesOutOfRange(String text) {
		assertThatThrownBy(() -> BoundingBox.parse(text)).isInstanceOf(InvalidBoundingBoxException.class)
			.hasMessage("Longitudes must be between -180 and 180");
	}

	@ParameterizedTest
	@ValueSource(strings = { "0,-90.1,1,1", "0,0,1,90.1" })
	void rejectsLatitudesOutOfRange(String text) {
		assertThatThrownBy(() -> BoundingBox.parse(text)).isInstanceOf(InvalidBoundingBoxException.class)
			.hasMessage("Latitudes must be between -90 and 90");
	}

	// Equal values would be an empty box; min greater than max would cross the antimeridian
	@ParameterizedTest
	@ValueSource(strings = { "1,0,1,1", "2,0,1,1", "0,1,1,1", "0,2,1,1" })
	void rejectsMinimumsNotBelowMaximums(String text) {
		assertThatThrownBy(() -> BoundingBox.parse(text)).isInstanceOf(InvalidBoundingBoxException.class)
			.hasMessage("The bounding box minimums must be less than its maximums");
	}

}
