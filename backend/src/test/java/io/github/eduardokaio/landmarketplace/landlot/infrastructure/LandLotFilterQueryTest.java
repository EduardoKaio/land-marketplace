package io.github.eduardokaio.landmarketplace.landlot.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.TestcontainersConfiguration;
import io.github.eduardokaio.landmarketplace.auth.AccountFixtures;
import io.github.eduardokaio.landmarketplace.landlot.LandLotFixtures;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotFilter;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The optional filters of the listing queries, run in PostGIS. Three lots of growing size and
 * price; the middle one is sold. Bounds are taken from the saved lots, so each test hits a bound
 * exactly and proves it is inclusive.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class LandLotFilterQueryTest {

	private static final double WIDE_RADIUS = 10_000;

	@Autowired
	private LandLotRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	private UUID ownerId;

	private LandLot small;

	private LandLot medium;

	private LandLot large;

	@BeforeEach
	void createLots() {
		ownerId = entityManager.persistFlushFind(AccountFixtures.account("owner@example.com")).getId();
		small = save(0, 1, "100000.00");
		medium = save(10, 2, "400000.00");
		medium.update(medium.getTitle(), medium.getDescription(), medium.getPrice(), medium.getContact(),
				LandLotStatus.SOLD);
		large = save(20, 3, "900000.00");
		repository.flush();
	}

	// Every parameter null: the case the CASTs in LandLotRepository.FILTERS are about
	@Test
	void withoutFiltersReturnsEveryLot() {
		assertThat(bbox(LandLotFilter.NONE)).containsExactly(large, medium, small);
		assertThat(circle(LandLotFilter.NONE)).containsExactlyInAnyOrder(small, medium, large);
	}

	@Test
	void filtersByPriceWithInclusiveBounds() {
		assertThat(bbox(filter().minPrice(new BigDecimal("400000")).build())).containsExactly(large, medium);
		assertThat(bbox(filter().maxPrice(new BigDecimal("400000.00")).build())).containsExactly(medium, small);
	}

	@Test
	void filtersByAreaWithInclusiveBounds() {
		assertThat(bbox(filter().minArea(medium.getAreaM2()).build())).containsExactly(large, medium);
		assertThat(bbox(filter().maxArea(medium.getAreaM2()).build())).containsExactly(medium, small);
	}

	// Compared with the value rounded to cents, the one the API shows
	@Test
	void filtersByThePricePerSquareMeterShown() {
		BigDecimal shown = medium.pricePerSquareMeter();

		assertThat(bbox(filter().maxPricePerSquareMeter(shown).build())).contains(medium);
		assertThat(bbox(filter().maxPricePerSquareMeter(shown.subtract(new BigDecimal("0.01"))).build()))
			.doesNotContain(medium);
	}

	@Test
	void filtersByStatus() {
		assertThat(bbox(filter().status(LandLotStatus.SOLD).build())).containsExactly(medium);
		assertThat(bbox(filter().status(LandLotStatus.AVAILABLE).build())).containsExactly(large, small);
	}

	@Test
	void combinesFilters() {
		LandLotFilter cheapAndAvailable = filter().maxPrice(new BigDecimal("500000"))
			.status(LandLotStatus.AVAILABLE)
			.build();

		assertThat(bbox(cheapAndAvailable)).containsExactly(small);
		assertThat(circle(cheapAndAvailable)).containsExactly(small);
	}

	// With the LIMIT before the filters, the largest lot would take the only row and be filtered out
	@Test
	void limitsTheListingAfterFiltering() {
		List<LandLot> found = repository.findInBoundingBox(-35.3, -6.0, -35.1, -5.8,
				filter().maxPrice(new BigDecimal("400000")).build(), 1);

		assertThat(found).containsExactly(medium);
	}

	private List<LandLot> bbox(LandLotFilter filter) {
		return repository.findInBoundingBox(-35.3, -6.0, -35.1, -5.8, filter, 10);
	}

	private List<LandLot> circle(LandLotFilter filter) {
		return repository.findWithinDistance(-35.2, -5.9, WIDE_RADIUS, filter);
	}

	/** Square of {@code size} 0.001 degree cells whose lower left corner is {@code offset} cells east. */
	private LandLot save(int offset, int size, String price) {
		double minLon = -35.2 + offset * 0.001;
		double maxLon = minLon + size * 0.001;
		double minLat = -5.9;
		double maxLat = minLat + size * 0.001;
		return repository.saveAndFlush(LandLot.create("Lot " + offset, "Desc", new BigDecimal(price),
				LandLotFixtures.contact(),
				LandLotFixtures.polygon(minLon, minLat, maxLon, minLat, maxLon, maxLat, minLon, maxLat, minLon, minLat),
				ownerId));
	}

	private static FilterBuilder filter() {
		return new FilterBuilder();
	}

	/** Keeps the tests readable: LandLotFilter has six positional fields. */
	private static final class FilterBuilder {

		private BigDecimal minPrice;

		private BigDecimal maxPrice;

		private Double minArea;

		private Double maxArea;

		private BigDecimal maxPricePerSquareMeter;

		private LandLotStatus status;

		FilterBuilder minPrice(BigDecimal value) {
			minPrice = value;
			return this;
		}

		FilterBuilder maxPrice(BigDecimal value) {
			maxPrice = value;
			return this;
		}

		FilterBuilder minArea(Double value) {
			minArea = value;
			return this;
		}

		FilterBuilder maxArea(Double value) {
			maxArea = value;
			return this;
		}

		FilterBuilder maxPricePerSquareMeter(BigDecimal value) {
			maxPricePerSquareMeter = value;
			return this;
		}

		FilterBuilder status(LandLotStatus value) {
			status = value;
			return this;
		}

		LandLotFilter build() {
			return new LandLotFilter(minPrice, maxPrice, minArea, maxArea, maxPricePerSquareMeter, status);
		}

	}

}
