package io.github.eduardokaio.landmarketplace.landlot.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotOverlapException;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotCreationLock;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoConfig;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import io.github.eduardokaio.landmarketplace.shared.geo.InvalidGeometryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.valid.TopologyValidationError;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandLotServiceTest {

	@Mock
	private LandLotRepository repository;

	@Mock
	private LandLotCreationLock creationLock;

	private LandLotService service;

	private final CreateLandLotCommand command = command(LandLotFixtures.pontaNegraSquare());

	@BeforeEach
	void setUp() {
		service = new LandLotService(repository, creationLock,
				new GeoJsonGeometryMapper(GeoConfig.newGeometryFactory()));
	}

	@Test
	void locksThenChecksOverlapThenSaves() {
		when(repository.findOverlappingIds(anyString())).thenReturn(List.of());
		when(repository.saveAndFlush(any(LandLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.create(command);

		InOrder order = inOrder(creationLock, repository);
		order.verify(creationLock).acquire();
		order.verify(repository).findOverlappingIds(anyString());
		order.verify(repository).saveAndFlush(any(LandLot.class));
	}

	@Test
	void savesLotBuiltFromTheCommandAndReturnsTheSavedEntity() {
		LandLot persisted = LandLotFixtures.pontaNegraLot();
		when(repository.findOverlappingIds(anyString())).thenReturn(List.of());
		when(repository.saveAndFlush(any(LandLot.class))).thenReturn(persisted);
		ArgumentCaptor<LandLot> saved = ArgumentCaptor.forClass(LandLot.class);

		LandLot result = service.create(command);

		verify(repository).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getTitle()).isEqualTo("Corner lot");
		assertThat(saved.getValue().getDescription()).isEqualTo("Flat terrain");
		assertThat(saved.getValue().getPrice()).isEqualByComparingTo("250000.00");
		assertThat(saved.getValue().getContact()).isEqualTo(LandLotFixtures.contact());
		assertThat(saved.getValue().getGeom().equalsExact(LandLotFixtures.pontaNegraSquare())).isTrue();
		assertThat(result).isSameAs(persisted);
	}

	@Test
	void sendsThePolygonToTheOverlapQueryAsGeoJsonText() {
		when(repository.findOverlappingIds(anyString())).thenReturn(List.of());
		when(repository.saveAndFlush(any(LandLot.class))).thenAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<String> geoJson = ArgumentCaptor.forClass(String.class);

		service.create(command);

		verify(repository).findOverlappingIds(geoJson.capture());
		assertThat(geoJson.getValue()).startsWith("{\"type\":\"Polygon\"").contains("-35.196").doesNotContain("crs");
	}

	@Test
	void rejectsOverlapWithEveryConflictingIdAndSavesNothing() {
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		when(repository.findOverlappingIds(anyString())).thenReturn(List.of(first, second));

		LandLotOverlapException ex = catchThrowableOfType(LandLotOverlapException.class,
				() -> service.create(command));

		assertThat(ex.getConflictingLotIds()).containsExactly(first, second);
		assertThat(ex).hasMessage("The submitted polygon overlaps 2 existing land lots.");
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void usesSingularMessageForOneConflict() {
		when(repository.findOverlappingIds(anyString())).thenReturn(List.of(UUID.randomUUID()));

		assertThatThrownBy(() -> service.create(command)).isInstanceOf(LandLotOverlapException.class)
			.hasMessage("The submitted polygon overlaps 1 existing land lot.");
	}

	// PostGIS predicates fail on invalid geometries, so the overlap query must never see one
	@Test
	void rejectsBowTieWithReasonAndLocationBeforeQueryingOverlaps() {
		assertThatThrownBy(() -> service.create(command(LandLotFixtures.bowTie())))
			.isInstanceOf(InvalidGeometryException.class)
			.hasMessage("Self-intersection at (0.5, 0.5)");

		verify(repository, never()).findOverlappingIds(anyString());
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsHoleOutsideShell() {
		Polygon shell = LandLotFixtures.polygon(0, 0, 10, 0, 10, 10, 0, 10, 0, 0);
		Polygon outside = LandLotFixtures.polygon(20, 20, 20, 22, 22, 22, 22, 20, 20, 20);
		Polygon withBadHole = LandLotFixtures.GEOMETRY_FACTORY.createPolygon(shell.getExteriorRing(),
				new LinearRing[] { outside.getExteriorRing() });

		assertThatThrownBy(() -> service.create(command(withBadHole))).isInstanceOf(InvalidGeometryException.class)
			.hasMessageStartingWith("Hole lies outside shell");
	}

	@Test
	void describesErrorWithoutLocationUsingOnlyTheReason() {
		TopologyValidationError error = new TopologyValidationError(TopologyValidationError.TOO_FEW_POINTS);

		assertThat(LandLotService.describe(error)).isEqualTo("Too few distinct points in geometry component");
	}

	private static CreateLandLotCommand command(Polygon geometry) {
		return new CreateLandLotCommand("Corner lot", "Flat terrain", new BigDecimal("250000.00"),
				LandLotFixtures.contact(), geometry);
	}

}
