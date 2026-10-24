package io.github.eduardokaio.landmarketplace.landlot.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotOverlapException;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoConfig;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoOverlapRuleTest {

	@Mock
	private LandLotRepository repository;

	private NoOverlapRule rule;

	private final CreateLandLotCommand command = new CreateLandLotCommand("Lot", "Desc", BigDecimal.TEN,
			LandLotFixtures.contact(), LandLotFixtures.pontaNegraSquare());

	@BeforeEach
	void setUp() {
		rule = new NoOverlapRule(repository, new GeoJsonGeometryMapper(GeoConfig.newGeometryFactory()));
	}

	@Test
	void passesWhenNoLotOverlaps() {
		when(repository.findOverlappingIds(anyString())).thenReturn(List.of());

		assertThatCode(() -> rule.validate(command)).doesNotThrowAnyException();
	}

	@Test
	void sendsThePolygonAsGeoJsonText() {
		when(repository.findOverlappingIds(anyString())).thenReturn(List.of());
		ArgumentCaptor<String> geoJson = ArgumentCaptor.forClass(String.class);

		rule.validate(command);

		verify(repository).findOverlappingIds(geoJson.capture());
		assertThat(geoJson.getValue()).startsWith("{\"type\":\"Polygon\"").contains("-35.196").doesNotContain("crs");
	}

	@Test
	void failsWithEveryConflictingId() {
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		when(repository.findOverlappingIds(anyString())).thenReturn(List.of(first, second));

		LandLotOverlapException ex = catchThrowableOfType(LandLotOverlapException.class, () -> rule.validate(command));

		assertThat(ex.getConflictingLotIds()).containsExactly(first, second);
		assertThat(ex).hasMessage("The submitted polygon overlaps 2 existing land lots.");
	}

	@Test
	void usesSingularMessageForOneConflict() {
		when(repository.findOverlappingIds(anyString())).thenReturn(List.of(UUID.randomUUID()));

		LandLotOverlapException ex = catchThrowableOfType(LandLotOverlapException.class, () -> rule.validate(command));

		assertThat(ex).hasMessage("The submitted polygon overlaps 1 existing land lot.");
	}

}
