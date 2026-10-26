import Circle from 'ol/geom/Circle'
import { fromLonLat } from 'ol/proj'
import { describe, expect, it } from 'vitest'
import { circleEdge, getCircleSearchParams } from './circleSearch'

function relativeDifference(actual: number, expected: number): number {
  return Math.abs(actual / expected - 1)
}

// ol/sphere measures on a sphere of radius 6,371,008.8 m while Web Mercator uses 6,378,137 m, so
// even at the equator one projected unit is about 0.1% more than one meter on the ground
const SPHERE_TOLERANCE = 0.005

describe('getCircleSearchParams', () => {
  it('returns the center in longitude and latitude', () => {
    const params = getCircleSearchParams(new Circle(fromLonLat([-35.196, -5.88]), 500))

    expect(params.lon).toBeCloseTo(-35.196, 9)
    expect(params.lat).toBeCloseTo(-5.88, 9)
  })

  it('matches the projected radius at the equator', () => {
    const circle = new Circle(fromLonLat([0, 0]), 1000)

    const { radiusMeters } = getCircleSearchParams(circle)

    expect(relativeDifference(radiusMeters, 1000)).toBeLessThan(SPHERE_TOLERANCE)
  })

  it('corrects the Web Mercator stretch at high latitude', () => {
    // At 60 degrees Web Mercator stretches distances by 1 / cos(60°) = 2
    const circle = new Circle(fromLonLat([10.75, 60]), 2000)

    const { radiusMeters } = getCircleSearchParams(circle)

    expect(relativeDifference(radiusMeters, 1000)).toBeLessThan(SPHERE_TOLERANCE)
    // Sending getRadius() as meters would search an area four times larger than the drawn one
    expect(circle.getRadius() / radiusMeters).toBeCloseTo(2, 2)
  })

  it('is zero for a circle without radius', () => {
    expect(getCircleSearchParams(new Circle(fromLonLat([0, 0]), 0)).radiusMeters).toBe(0)
  })
})

describe('circleEdge', () => {
  it('is the point east of the center at radius distance', () => {
    expect(circleEdge(new Circle([100, 200], 50))).toEqual([150, 200])
  })
})
