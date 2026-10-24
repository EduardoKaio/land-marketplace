package io.github.eduardokaio.landmarketplace.landlot.application;

import java.util.List;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotCreationLock;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LandLotService {

	private final LandLotRepository repository;

	private final LandLotCreationLock creationLock;

	private final List<LandLotRule> rules;

	public LandLotService(LandLotRepository repository, LandLotCreationLock creationLock, List<LandLotRule> rules) {
		this.repository = repository;
		this.creationLock = creationLock;
		this.rules = List.copyOf(rules);
	}

	@Transactional
	public LandLot create(CreateLandLotCommand command) {
		// First statement of the transaction, so the overlap check and the insert run under the lock
		creationLock.acquire();
		rules.forEach(rule -> rule.validate(command));
		LandLot lot = LandLot.create(command.title(), command.description(), command.price(), command.contact(),
				command.geometry());
		// Flush inside the transaction so the generated area is read back and database errors surface here
		return repository.saveAndFlush(lot);
	}

}
