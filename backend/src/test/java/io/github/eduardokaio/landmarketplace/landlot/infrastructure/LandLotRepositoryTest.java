package io.github.eduardokaio.landmarketplace.landlot.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.TestcontainersConfiguration;
import io.github.eduardokaio.landmarketplace.auth.AccountFixtures;
import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.landlot.api.LandLotFeature;
import io.github.eduardokaio.landmarketplace.landlot.api.LandLotFeatureCollection;
import io.github.eduardokaio.landmarketplace.landlot.api.LandLotGeoJsonAssembler;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotFilter;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotStatus;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoConfig;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.withinPercentage;

/**
 * Runs against a real PostGIS (Testcontainers) because area, CHECK constraints and spatial types
 * only exist in the database.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, GeoConfig.class, GeoJsonGeometryMapper.class,
		LandLotGeoJsonAssembler.class })
class LandLotRepositoryTest {

	@Autowired
	private LandLotRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private LandLotGeoJsonAssembler assembler;

	/** Every lot needs an existing account as owner: owner_id is a foreign key. */
	private UUID ownerId;

	@BeforeEach
	void createOwner() {
		ownerId = entityManager.persistFlushFind(AccountFixtures.account("owner@example.com")).getId();
	}

	@Test
	void savesAndReadsLotWithAreaComputedByPostgis() {
		LandLot saved = repository.saveAndFlush(LandLotFixtures.pontaNegraLot(ownerId));
		entityManager.clear();

		LandLot found = repository.findById(saved.getId()).orElseThrow();

		assertThat(found.getTitle()).isEqualTo("Corner lot near Ponta Negra");
		assertThat(found.getDescription()).isEqualTo("Flat terrain, ready to build, two blocks from the beach.");
		assertThat(found.getPrice()).isEqualByComparingTo("250000.00");
		assertThat(found.getContact()).isEqualTo(LandLotFixtures.contact());
		assertThat(found.getStatus()).isEqualTo(LandLotStatus.AVAILABLE);
		assertThat(found.getGeom().equalsExact(LandLotFixtures.pontaNegraSquare())).isTrue();
		assertThat(found.getGeom().getSRID()).isEqualTo(GeoConfig.SRID);
		assertThat(found.getAreaM2()).isCloseTo(LandLotFixtures.PONTA_NEGRA_AREA_M2, withinPercentage(0.1));
		assertThat(found.pricePerSquareMeter()).isEqualByComparingTo("20.41");
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getUpdatedAt()).isNotNull();
	}

	@Test
	void areaIsAvailableRightAfterInsertWithoutReloading() {
		LandLot saved = repository.saveAndFlush(LandLotFixtures.pontaNegraLot(ownerId));

		assertThat(saved.getAreaM2()).isCloseTo(LandLotFixtures.PONTA_NEGRA_AREA_M2, withinPercentage(0.1));
	}

	@Test
	void databaseRejectsInvalidGeometryEvenWhenTheDomainIsBypassed() {
		var insert = entityManager.getEntityManager().createNativeQuery("""
				INSERT INTO land_lot (id, title, description, price, contact_name, contact_phone, geom)
				VALUES (gen_random_uuid(), 'Bow tie', 'Invalid', 1, 'Kaio', '123',
				        ST_GeomFromText('POLYGON((0 0, 1 1, 1 0, 0 1, 0 0))', 4326))
				""");

		assertThatThrownBy(insert::executeUpdate).isInstanceOf(PersistenceException.class)
			.hasMessageContaining("land_lot_geom_check");
	}

	@Test
	void buildsGeoJsonFeatureFollowingTheApiContract() {
		LandLot saved = repository.saveAndFlush(LandLotFixtures.pontaNegraLot(ownerId));

		LandLotFeatureCollection collection = assembler.toCollection(List.of(saved));
		JsonNode json = JsonMapper.shared().valueToTree(collection);

		assertThat(json.get("type").stringValue()).isEqualTo("FeatureCollection");
		JsonNode feature = json.get("features").get(0);
		assertThat(feature.get("type").stringValue()).isEqualTo("Feature");
		assertThat(feature.get("id").stringValue()).isEqualTo(saved.getId().toString());
		assertThat(feature.get("geometry").get("type").stringValue()).isEqualTo("Polygon");
		JsonNode properties = feature.get("properties");
		assertThat(properties.propertyNames()).containsExactly("title", "price", "areaSquareMeters",
				"pricePerSquareMeter", "description", "contact", "status", "createdAt");
		assertThat(properties.get("price").decimalValue()).isEqualByComparingTo("250000.00");
		assertThat(properties.get("areaSquareMeters").doubleValue()).isCloseTo(LandLotFixtures.PONTA_NEGRA_AREA_M2,
				withinPercentage(0.1));
		assertThat(properties.get("pricePerSquareMeter").decimalValue()).isEqualByComparingTo("20.41");
		assertThat(properties.get("contact").get("email").stringValue()).isEqualTo("kaio@example.com");
		assertThat(properties.get("status").stringValue()).isEqualTo("AVAILABLE");
		assertThat(properties.get("createdAt").stringValue()).endsWith("Z");
	}

	@Test
	void featureTypeIsFixed() {
		LandLot saved = repository.saveAndFlush(LandLotFixtures.pontaNegraLot(ownerId));

		LandLotFeature feature = assembler.toFeature(saved);

		assertThat(feature.type()).isEqualTo("Feature");
		assertThat(feature.id()).isEqualTo(saved.getId());
	}

	@Test
	void boundingBoxQueryReturnsLargestLotsFirstUpToTheLimit() {
		LandLot small = repository.save(squareLot(0, 1));
		LandLot large = repository.save(squareLot(10, 3));
		LandLot medium = repository.save(squareLot(20, 2));
		repository.flush();
		entityManager.clear();

		List<LandLot> found = repository.findInBoundingBox(-35.3, -6.0, -35.1, -5.8, LandLotFilter.NONE, 2);

		assertThat(found).extracting(LandLot::getId).containsExactly(large.getId(), medium.getId());
		assertThat(found).extracting(LandLot::getId).doesNotContain(small.getId());
	}

	@Test
	void boundingBoxQueryIgnoresLotsOutsideTheBox() {
		repository.saveAndFlush(squareLot(0, 1));

		assertThat(repository.findInBoundingBox(-35.0, -6.0, -34.9, -5.8, LandLotFilter.NONE, 10)).isEmpty();
	}

	@Test
	void distanceQueryMeasuresInMeters() {
		LandLot lot = repository.saveAndFlush(squareLot(0, 1));

		// The lot starts at -35.2 and the center is 0.001 degree (about 110 m) to the west
		assertThat(repository.findWithinDistance(-35.201, -5.9, 100, LandLotFilter.NONE)).isEmpty();
		assertThat(repository.findWithinDistance(-35.201, -5.9, 120, LandLotFilter.NONE)).extracting(LandLot::getId)
			.containsExactly(lot.getId());
	}

	/** Square of {@code size} 0.001 degree cells whose lower left corner is {@code offset} cells east. */
	private LandLot squareLot(int offset, int size) {
		double minLon = -35.2 + offset * 0.001;
		double maxLon = minLon + size * 0.001;
		double minLat = -5.9;
		double maxLat = minLat + size * 0.001;
		return LandLot.create("Lot", "Desc", new BigDecimal("1000.00"), LandLotFixtures.contact(), LandLotFixtures
			.polygon(minLon, minLat, maxLon, minLat, maxLon, maxLat, minLon, maxLat, minLon, minLat), ownerId);
	}

	@Test
	void findsTheLotsOfOneOwnerNewestFirst() {
		UUID otherOwnerId = entityManager.persistFlushFind(AccountFixtures.account("other@example.com")).getId();
		LandLot older = repository.saveAndFlush(squareLot(0, 1));
		LandLot newer = repository.saveAndFlush(squareLot(10, 1));
		repository.saveAndFlush(LandLot.create("Not mine", "Desc", new BigDecimal("1000.00"), LandLotFixtures.contact(),
				LandLotFixtures.polygon(-35.0, -5.9, -34.999, -5.9, -34.999, -5.899, -35.0, -5.899, -35.0, -5.9),
				otherOwnerId));

		assertThat(repository.findByOwnerIdOrderByCreatedAtDesc(ownerId)).extracting(LandLot::getId)
			.containsExactly(newer.getId(), older.getId());
	}

	@Test
	void databaseRejectsAnOwnerThatIsNotAnAccount() {
		LandLot orphan = LandLotFixtures.pontaNegraLot(UUID.randomUUID());

		assertThatThrownBy(() -> repository.saveAndFlush(orphan)).hasMessageContaining("owner_id");
	}

}
