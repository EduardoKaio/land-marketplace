import type { Coordinate } from 'ol/coordinate'
import type Circle from 'ol/geom/Circle'
import { toLonLat } from 'ol/proj'
import { getDistance } from 'ol/sphere'

export interface CircleSearchParams {
  lon: number
  lat: number
  /** Ground distance from the center to the border, in meters. */
  radiusMeters: number
}

/** The easternmost point of the circle: same y as the center, so it lies on the same latitude. */
export function circleEdge(circle: Circle): Coordinate {
  const [x, y] = circle.getCenter()
  return [x + circle.getRadius(), y]
}

/**
 * A circle drawn on the map (EPSG:3857) as the API search: center in WGS84 and real radius.
 *
 * circle.getRadius() is not used as the radius on purpose: it is measured in Web Mercator units,
 * and Web Mercator stretches distances by 1 / cos(latitude). At the equator one unit is about one
 * meter, but at 60 degrees a circle with radius 2000 covers only about 1000 m of ground, so
 * sending getRadius() would search twice as far as the circle the user sees. Measuring the
 * geodesic distance between the center and a point of the border, both in lon/lat, gives the
 * ground radius at any latitude. Web Mercator keeps shapes locally (it is conformal), so the
 * drawn circle is close to the true ground circle that PostGIS searches.
 */
export function getCircleSearchParams(circle: Circle): CircleSearchParams {
  const center = toLonLat(circle.getCenter())
  const edge = toLonLat(circleEdge(circle))
  const [lon, lat] = center
  return { lon, lat, radiusMeters: getDistance(center, edge) }
}
