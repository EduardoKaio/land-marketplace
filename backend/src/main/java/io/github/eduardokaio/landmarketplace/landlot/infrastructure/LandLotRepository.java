package io.github.eduardokaio.landmarketplace.landlot.infrastructure;

import java.util.List;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LandLotRepository extends JpaRepository<LandLot, UUID> {

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
	 * <p>
	 * Distances are geodesic meters on the WGS84 spheroid. The expression l.geom::geography is
	 * written exactly as in the land_lot_geog_gist index; any other form would not use the index.
	 */
	@Query(value = """
			SELECT l.*
			FROM land_lot l
			WHERE ST_DWithin(l.geom::geography,
			                 ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
			                 :radius)
			""", nativeQuery = true)
	List<LandLot> findWithinDistance(@Param("lon") double lon, @Param("lat") double lat,
			@Param("radius") double radiusMeters);

	/**
	 * Lots whose bounding box intersects the given rectangle, largest first, at most {@code limit}.
	 * <p>
	 * && only compares bounding boxes, which is enough for a map viewport and the cheapest check
	 * the GiST index on geom can answer. Largest first because, when zoomed out, small lots are
	 * smaller than a pixel; the id makes the order deterministic.
	 */
	@Query(value = """
			SELECT l.*
			FROM land_lot l
			WHERE l.geom && ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
			ORDER BY l.area_m2 DESC, l.id
			LIMIT :limit
			""", nativeQuery = true)
	List<LandLot> findInBoundingBox(@Param("minLon") double minLon, @Param("minLat") double minLat,
			@Param("maxLon") double maxLon, @Param("maxLat") double maxLat, @Param("limit") int limit);

}
