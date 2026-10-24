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

}
