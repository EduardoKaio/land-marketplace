package io.github.eduardokaio.landmarketplace.shared.geo;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GeoConfig {

	/** WGS84 lon/lat, used in the database and in the API. */
	public static final int SRID = 4326;

	@Bean
	GeometryFactory geometryFactory() {
		return new GeometryFactory(new PrecisionModel(), SRID);
	}

}
