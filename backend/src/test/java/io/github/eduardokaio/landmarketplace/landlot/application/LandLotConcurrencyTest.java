package io.github.eduardokaio.landmarketplace.landlot.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.github.eduardokaio.landmarketplace.TestcontainersConfiguration;
import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotOverlapException;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the advisory lock: many requests for the same polygon released at the same instant must
 * produce exactly one lot. Without the lock several transactions would pass the overlap check
 * before any of them commits.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LandLotConcurrencyTest {

	private static final int THREADS = 8;

	@Autowired
	private LandLotService service;

	@Autowired
	private LandLotRepository repository;

	private final ExecutorService executor = Executors.newFixedThreadPool(THREADS);

	@BeforeEach
	void cleanDatabase() {
		repository.deleteAll();
	}

	@AfterEach
	void shutdown() {
		executor.shutdownNow();
		repository.deleteAll();
	}

	@Test
	void concurrentCreationsOfTheSamePolygonProduceExactlyOneLot() throws Exception {
		CreateLandLotCommand command = new CreateLandLotCommand("Corner lot", "Flat terrain",
				new BigDecimal("250000.00"), LandLotFixtures.contact(), LandLotFixtures.pontaNegraSquare());
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < THREADS; i++) {
			results.add(executor.submit(() -> {
				start.await();
				try {
					service.create(command);
					return true;
				}
				catch (LandLotOverlapException ex) {
					return false;
				}
			}));
		}

		start.countDown();
		int created = 0;
		for (Future<Boolean> result : results) {
			if (result.get(30, TimeUnit.SECONDS)) {
				created++;
			}
		}

		assertThat(created).isEqualTo(1);
		assertThat(repository.count()).isEqualTo(1);
	}

}
