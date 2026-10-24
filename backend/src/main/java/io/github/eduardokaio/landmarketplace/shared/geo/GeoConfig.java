package io.github.eduardokaio.landmarketplace.shared.geo;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GeoConfig {

	/** WGS84 lon/lat, used in the database and in the API. */
	public static final int SRID = 4326;

	/** Coordinates are kept on a 1e-7 degree grid (about 1.1 cm at the equator). */
	public static final int COORDINATE_DECIMALS = 7;

	/**
	 * Fixed precision grid. Rounding every incoming coordinate to the same grid makes a vertex shared
	 * by two neighbors identical bit for bit in the database, in the overlap query and in API
	 * responses, so floating point noise (for example from the map projection round trip) cannot
	 * turn a shared border into a tiny overlap or gap.
	 */
	public static final PrecisionModel PRECISION_MODEL = new PrecisionModel(Math.pow(10, COORDINATE_DECIMALS));

	public static GeometryFactory newGeometryFactory() {
		return new GeometryFactory(PRECISION_MODEL, SRID);
	}

	@Bean
	GeometryFactory geometryFactory() {
		return newGeometryFactory();
	}

}
