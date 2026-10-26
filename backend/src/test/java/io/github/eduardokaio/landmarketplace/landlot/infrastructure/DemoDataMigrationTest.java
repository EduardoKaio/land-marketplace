package io.github.eduardokaio.landmarketplace.landlot.infrastructure;

import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.TestcontainersConfiguration;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The demo seed is inserted with plain SQL, bypassing the overlap check of the service. These tests
 * run the same checks against it, so the sample data could also have been created through the API.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("demo")
class DemoDataMigrationTest {

	private static final UUID PONTA_NEGRA_WEST = UUID.fromString("a0000000-0000-4000-8000-000000000001");

	private static final UUID PONTA_NEGRA_EAST = UUID.fromString("a0000000-0000-4000-8000-000000000002");

	private static final UUID DEMO_ACCOUNT = UUID.fromString("b0000000-0000-4000-8000-000000000001");

	@Autowired
	private LandLotRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void loadsSampleLotsAroundNatal() {
		Number outsideNatal = (Number) entityManager.createNativeQuery("""
				SELECT count(*) FROM land_lot
				WHERE NOT ST_Within(geom, ST_MakeEnvelope(-35.30, -5.95, -35.10, -5.70, 4326))
				""").getSingleResult();

		assertThat(repository.count()).isEqualTo(6);
		assertThat(outsideNatal.longValue()).isZero();
	}

	@Test
	void noSampleLotOverlapsAnother() {
		for (UUID id : repository.findAll().stream().map(LandLot::getId).toList()) {
			String geometry = (String) entityManager
				.createNativeQuery("SELECT ST_AsGeoJSON(geom) FROM land_lot WHERE id = :id")
				.setParameter("id", id)
				.getSingleResult();

			// The rule the API applies on create: each lot overlaps only itself
			assertThat(repository.findOverlappingIds(geometry)).as("lots overlapping %s", id).containsExactly(id);
		}
	}

	@Test
	void pontaNegraNeighborsShareABorder() {
		Object[] relation = (Object[]) entityManager.createNativeQuery("""
				SELECT ST_Touches(w.geom, e.geom), ST_Length(ST_Intersection(w.geom, e.geom)::geography)
				FROM land_lot w, land_lot e
				WHERE w.id = :west AND e.id = :east
				""").setParameter("west", PONTA_NEGRA_WEST).setParameter("east", PONTA_NEGRA_EAST).getSingleResult();

		assertThat(relation[0]).isEqualTo(true);
		// A whole edge in common (0.0015 degree of latitude, about 166 m), not just a corner
		assertThat(((Number) relation[1]).doubleValue()).isBetween(160.0, 170.0);
	}

	@Test
	void keepsOneSoldLot() {
		List<LandLotStatus> statuses = repository.findAll().stream().map(LandLot::getStatus).toList();

		assertThat(statuses).containsOnlyOnce(LandLotStatus.SOLD);
	}

	@Test
	void everySampleLotBelongsToTheDemoAccount() {
		assertThat(repository.findAll()).allSatisfy(lot -> assertThat(lot.getOwnerId()).isEqualTo(DEMO_ACCOUNT));
	}

	// The README promises this password; a wrong hash in the seed would fail only at login time
	@Test
	void demoAccountPasswordIsTheDocumentedOne() {
		String hash = (String) entityManager
			.createNativeQuery("SELECT password_hash FROM app_user WHERE email = 'demo@example.com'")
			.getSingleResult();

		assertThat(new BCryptPasswordEncoder().matches("demo1234", hash)).isTrue();
	}

}
