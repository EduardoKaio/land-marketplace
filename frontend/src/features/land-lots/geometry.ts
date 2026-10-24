import GeoJSON from 'ol/format/GeoJSON'
import type Polygon from 'ol/geom/Polygon'
import type { PolygonGeometry } from '../../api/types'

/** Same grid as the backend: 1e-7 degree, about 1 cm. */
const COORDINATE_DECIMALS = 7

const geoJson = new GeoJSON()

/**
 * The drawn polygon (map projection) as the API expects it: GeoJSON in WGS84.
 *
 * Rounding to the backend grid matters for neighbors: a vertex snapped to an existing lot comes
 * back from Web Mercator with noise around 1e-9 degree, and rounding restores the exact value of
 * the neighbor's vertex, so a shared border stays a shared border. rightHanded writes the exterior
 * ring counterclockwise, as RFC 7946 asks.
 */
export function toApiGeometry(polygon: Polygon): PolygonGeometry {
  return geoJson.writeGeometryObject(polygon, {
    dataProjection: 'EPSG:4326',
    featureProjection: 'EPSG:3857',
    decimals: COORDINATE_DECIMALS,
    rightHanded: true,
  }) as PolygonGeometry
}
