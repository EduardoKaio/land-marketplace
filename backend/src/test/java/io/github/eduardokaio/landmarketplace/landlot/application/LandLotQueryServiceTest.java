package io.github.eduardokaio.landmarketplace.landlot.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotNotFoundException;
import io.github.eduardokaio.landmarketplace.landlot.domain.SearchRadiusTooLargeException;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import io.github.eduardokaio.landmarketplace.shared.geo.BoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LandLotQueryServiceTest {

	private static final BoundingBox BOX = new BoundingBox(-35.21, -5.91, -35.19, -5.89);

	@Mock
	private LandLotRepository repository;

	private LandLotQueryService service;

	private final LandLot first = LandLotFixtures.pontaNegraLot();

	private final LandLot second = LandLotFixtures.pontaNegraLot();

	private final LandLot third = LandLotFixtures.pontaNegraLot();

	@BeforeEach
	void setUp() {
		service = new LandLotQueryService(repository, new LandLotQueryProperties(50_000, 2));
	}

	@Test
	void searchPassesTheCircleToTheRepository() {
		when(repository.findWithinDistance(-35.2, -5.9, 1000)).thenReturn(List.of(first));

		assertThat(service.search(-35.2, -5.9, 1000)).containsExactly(first);
	}

	@Test
	void searchAcceptsRadiusEqualToTheMaximum() {
		when(repository.findWithinDistance(-35.2, -5.9, 50_000)).thenReturn(List.of());

		assertThat(service.search(-35.2, -5.9, 50_000)).isEmpty();
	}

	@Test
	void searchRejectsRadiusAboveTheMaximumWithoutQuerying() {
		assertThatThrownBy(() -> service.search(-35.2, -5.9, 50_000.1))
			.isInstanceOf(SearchRadiusTooLargeException.class)
			.hasMessage("The search radius must be at most 50000 meters.")
			.extracting("maxRadiusMeters")
			.isEqualTo(50_000.0);
		verify(repository, never()).findWithinDistance(anyDouble(), anyDouble(), anyDouble());
	}

	@Test
	void bboxAsksForOneRowMoreThanTheCapAndFlagsTruncation() {
		when(repository.findInBoundingBox(-35.21, -5.91, -35.19, -5.89, 3)).thenReturn(List.of(first, second, third));

		LandLotSlice slice = service.findInBoundingBox(BOX);

		assertThat(slice.lots()).containsExactly(first, second);
		assertThat(slice.truncated()).isTrue();
	}

	@Test
	void bboxIsNotTruncatedWhenEverythingFits() {
		when(repository.findInBoundingBox(-35.21, -5.91, -35.19, -5.89, 3)).thenReturn(List.of(first, second));

		LandLotSlice slice = service.findInBoundingBox(BOX);

		assertThat(slice.lots()).containsExactly(first, second);
		assertThat(slice.truncated()).isFalse();
	}

	@Test
	void getByIdReturnsTheLot() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.of(first));

		assertThat(service.getById(id)).isSameAs(first);
	}

	@Test
	void getByIdThrowsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(id)).isInstanceOf(LandLotNotFoundException.class)
			.hasMessage("No land lot with id " + id + ".");
	}

}
