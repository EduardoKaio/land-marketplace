package io.github.eduardokaio.landmarketplace.landlot.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLotFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LandLotRepository extends JpaRepository<LandLot, UUID> {

	/**
	 * Optional filters shared by the listing queries; see {@link LandLotFilter}. Each one is
	 * skipped when its parameter is null.
	 * <p>
	 * Every parameter is wrapped in CAST, also where it is compared with a column. PostgreSQL cannot
	 * infer the type of "$1 IS NULL" and fails with "could not determine data type of parameter $1"
	 * when the driver sends the null without a type. Spring Data happens to send nulls typed with
	 * the Java type of the method parameter, so these queries also work without the CASTs (checked
	 * while writing them), but that is a detail of the caller; with the CASTs the SQL is correct on
	 * its own, whoever binds it. The price per m² is rounded to cents like the API shows it, so a
	 * lot shown as R$ 20.41 matches a maximum of 20.41.
	 */
	String FILTERS = """
			  AND (CAST(:minPrice AS numeric) IS NULL OR l.price >= CAST(:minPrice AS numeric))
			  AND (CAST(:maxPrice AS numeric) IS NULL OR l.price <= CAST(:maxPrice AS numeric))
			  AND (CAST(:minArea AS double precision) IS NULL OR l.area_m2 >= CAST(:minArea AS double precision))
			  AND (CAST(:maxArea AS double precision) IS NULL OR l.area_m2 <= CAST(:maxArea AS double precision))
			  AND (CAST(:maxPricePerSquareMeter AS numeric) IS NULL
			       OR ROUND(l.price / CAST(l.area_m2 AS numeric), 2) <= CAST(:maxPricePerSquareMeter AS numeric))
			  AND (CAST(:status AS varchar) IS NULL OR l.status = CAST(:status AS varchar))
			""";

	/**
	 * Ids of lots whose interior shares area with the given polygon.
	 * <p>
	 * "Intersects and does not only touch" means the interiors cross: partial overlap, containment
	 * in either direction and identical shapes all match, while neighbors that only share a border
	 * or a vertex do not. ST_Overlaps is not used because it is false when one polygon contains the
	 * other. ST_Intersects uses the GiST index on geom through its implicit bounding box check.
	 * <p>
	 * The polygon is passed as GeoJSON text to avoid binding JTS types in a native query.
	 */
	@Query(value = """
			WITH candidate AS (SELECT ST_SetSRID(ST_GeomFromGeoJSON(:geometry), 4326) AS geom)
			SELECT l.id
			FROM land_lot l, candidate c
			WHERE ST_Intersects(l.geom, c.geom)
			  AND NOT ST_Touches(l.geom, c.geom)
			ORDER BY l.id
			""", nativeQuery = true)
	List<UUID> findOverlappingIds(@Param("geometry") String geoJsonGeometry);

	/**
	 * Lots with at least one point within the given distance of the center, which is the same as
	 * intersecting the circle: lots crossing its edge or containing it entirely also match.
	 */
	default List<LandLot> findWithinDistance(double lon, double lat, double radiusMeters, LandLotFilter filter) {
		return searchWithinDistance(lon, lat, radiusMeters, filter.minPrice(), filter.maxPrice(), filter.minArea(),
				filter.maxArea(), filter.maxPricePerSquareMeter(), statusOf(filter));
	}

	/**
	 * See {@link #findWithinDistance(double, double, double, LandLotFilter)}, which unpacks the
	 * filter. Distances are geodesic meters on the WGS84 spheroid. The expression l.geom::geography
	 * is written exactly as in the land_lot_geog_gist index; any other form would not use the index.
	 */
	@Query(value = """
			SELECT l.*
			FROM land_lot l
			WHERE ST_DWithin(l.geom::geography,
			                 ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
			                 :radius)
			""" + FILTERS, nativeQuery = true)
	List<LandLot> searchWithinDistance(@Param("lon") double lon, @Param("lat") double lat,
			@Param("radius") double radiusMeters, @Param("minPrice") BigDecimal minPrice,
			@Param("maxPrice") BigDecimal maxPrice, @Param("minArea") Double minArea, @Param("maxArea") Double maxArea,
			@Param("maxPricePerSquareMeter") BigDecimal maxPricePerSquareMeter, @Param("status") String status);

	/**
	 * Lots whose bounding box intersects the given rectangle, largest first, at most {@code limit}.
	 */
	default List<LandLot> findInBoundingBox(double minLon, double minLat, double maxLon, double maxLat,
			LandLotFilter filter, int limit) {
		return searchInBoundingBox(minLon, minLat, maxLon, maxLat, filter.minPrice(), filter.maxPrice(),
				filter.minArea(), filter.maxArea(), filter.maxPricePerSquareMeter(), statusOf(filter), limit);
	}

	/**
	 * See {@link #findInBoundingBox(double, double, double, double, LandLotFilter, int)}, which
	 * unpacks the filter.
	 * <p>
	 * && only compares bounding boxes, which is enough for a map viewport and the cheapest check
	 * the GiST index on geom can answer. Largest first because, when zoomed out, small lots are
	 * smaller than a pixel; the id makes the order deterministic. The LIMIT applies after the
	 * filters, so a filtered listing is truncated only when more matching lots exist.
	 */
	@Query(value = """
			SELECT l.*
			FROM land_lot l
			WHERE l.geom && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
			""" + FILTERS + """
			ORDER BY l.area_m2 DESC, l.id
			LIMIT :limit
			""", nativeQuery = true)
	List<LandLot> searchInBoundingBox(@Param("minLon") double minLon, @Param("minLat") double minLat,
			@Param("maxLon") double maxLon, @Param("maxLat") double maxLat, @Param("minPrice") BigDecimal minPrice,
			@Param("maxPrice") BigDecimal maxPrice, @Param("minArea") Double minArea, @Param("maxArea") Double maxArea,
			@Param("maxPricePerSquareMeter") BigDecimal maxPricePerSquareMeter, @Param("status") String status,
			@Param("limit") int limit);

	/** Derived query; served by the land_lot_owner_id_idx index. */
	List<LandLot> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	/** The column stores the enum name. */
	private static String statusOf(LandLotFilter filter) {
		return filter.status() == null ? null : filter.status().name();
	}

}
