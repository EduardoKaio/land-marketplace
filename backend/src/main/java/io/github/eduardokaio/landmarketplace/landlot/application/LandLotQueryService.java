package io.github.eduardokaio.landmarketplace.landlot.application;

import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotFilter;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotNotFoundException;
import io.github.eduardokaio.landmarketplace.landlot.domain.SearchRadiusTooLargeException;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import io.github.eduardokaio.landmarketplace.shared.geo.BoundingBox;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of land lots. Separate from LandLotService because reads need neither the creation
 * lock nor the creation rules.
 */
@Service
@Transactional(readOnly = true)
public class LandLotQueryService {

	private final LandLotRepository repository;

	private final LandLotQueryProperties properties;

	public LandLotQueryService(LandLotRepository repository, LandLotQueryProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	public List<LandLot> search(double lon, double lat, double radiusMeters, LandLotFilter filter) {
		if (radiusMeters > properties.maxSearchRadiusMeters()) {
			throw new SearchRadiusTooLargeException(properties.maxSearchRadiusMeters());
		}
		return repository.findWithinDistance(lon, lat, radiusMeters, filter);
	}

	public LandLotSlice findInBoundingBox(BoundingBox box, LandLotFilter filter) {
		int max = properties.maxBboxResults();
		// One extra row tells whether the cap cut anything off, without a separate COUNT query
		List<LandLot> lots = repository.findInBoundingBox(box.minLon(), box.minLat(), box.maxLon(), box.maxLat(),
				filter, max + 1);
		boolean truncated = lots.size() > max;
		return new LandLotSlice(truncated ? lots.subList(0, max) : lots, truncated);
	}

	/** The lots of one account, newest first. */
	public List<LandLot> findByOwner(UUID ownerId) {
		return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
	}

	public LandLot getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new LandLotNotFoundException(id));
	}

}
