package io.github.eduardokaio.landmarketplace.landlot.application;

import java.math.BigDecimal;
import java.util.List;

import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotOverlapException;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotCreationLock;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

	@Mock
	private LandLotRule firstRule;

	@Mock
	private LandLotRule secondRule;

	private LandLotService service;

	private final CreateLandLotCommand command = new CreateLandLotCommand("Corner lot", "Flat terrain",
			new BigDecimal("250000.00"), LandLotFixtures.contact(), LandLotFixtures.pontaNegraSquare());

	@BeforeEach
	void setUp() {
		service = new LandLotService(repository, creationLock, List.of(firstRule, secondRule));
	}

	@Test
	void locksThenRunsEveryRuleInOrderThenSaves() {
		when(repository.saveAndFlush(any(LandLot.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.create(command);

		InOrder order = inOrder(creationLock, firstRule, secondRule, repository);
		order.verify(creationLock).acquire();
		order.verify(firstRule).validate(command);
		order.verify(secondRule).validate(command);
		order.verify(repository).saveAndFlush(any(LandLot.class));
	}

	@Test
	void savesLotBuiltFromTheCommandAndReturnsTheSavedEntity() {
		LandLot persisted = LandLotFixtures.pontaNegraLot();
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
	void stopsAtTheFirstViolatedRuleAndSavesNothing() {
		LandLotOverlapException violation = new LandLotOverlapException(List.of());
		doThrow(violation).when(firstRule).validate(command);

		assertThatThrownBy(() -> service.create(command)).isSameAs(violation);

		verify(secondRule, never()).validate(any());
		verify(repository, never()).saveAndFlush(any());
	}

}
