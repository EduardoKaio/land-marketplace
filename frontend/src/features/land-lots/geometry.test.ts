import Polygon from 'ol/geom/Polygon'
import { fromLonLat } from 'ol/proj'
import { describe, expect, it } from 'vitest'
import { PONTA_NEGRA_RING, pontaNegraPolygon } from '../../test/fixtures'
import { toApiGeometry } from './geometry'

describe('toApiGeometry', () => {
  it('converts the drawn polygon back to WGS84 GeoJSON', () => {
    expect(toApiGeometry(pontaNegraPolygon())).toEqual({
      type: 'Polygon',
      coordinates: [PONTA_NEGRA_RING],
    })
  })

  // A vertex snapped to a neighbor goes 4326 -> 3857 -> 4326; without rounding it would differ
  // from the neighbor's vertex in the last digits and the shared border would become an overlap
  it('returns the exact backend coordinates of a vertex taken from an existing lot', () => {
    const vertex: [number, number] = [-35.1950123, -5.8800001]
    const roundTrip = toApiGeometry(
      new Polygon([
        [
          fromLonLat(vertex),
          fromLonLat([-35.19, -5.88]),
          fromLonLat([-35.19, -5.87]),
          fromLonLat(vertex),
        ],
      ]),
    )

    expect(roundTrip.coordinates[0][0]).toEqual(vertex)
  })

  it('keeps at most 7 decimals, the backend grid', () => {
    const polygon = new Polygon([
      [
        fromLonLat([-35.123456789, -5.1]),
        fromLonLat([-35.1, -5.1]),
        fromLonLat([-35.1, -5.0]),
        fromLonLat([-35.123456789, -5.1]),
      ],
    ])

    expect(toApiGeometry(polygon).coordinates[0][0][0]).toBe(-35.1234568)
  })

  it('writes the exterior ring counterclockwise even when drawn clockwise', () => {
    const clockwise = new Polygon([
      [...PONTA_NEGRA_RING].reverse().map((position) => fromLonLat(position)),
    ])

    const [ring] = toApiGeometry(clockwise).coordinates

    expect(signedArea(ring)).toBeGreaterThan(0)
  })
})

/** Shoelace formula: positive for counterclockwise rings. */
function signedArea(ring: number[][]): number {
  let sum = 0
  for (let i = 0; i < ring.length - 1; i++) {
    sum += ring[i][0] * ring[i + 1][1] - ring[i + 1][0] * ring[i][1]
  }
  return sum / 2
}
