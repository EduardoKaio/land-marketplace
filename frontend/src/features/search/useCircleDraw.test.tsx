import { act, renderHook } from '@testing-library/react'
import Feature from 'ol/Feature'
import type OlMap from 'ol/Map'
import Circle from 'ol/geom/Circle'
import Draw, { DrawEvent } from 'ol/interaction/Draw'
import type VectorLayer from 'ol/layer/Vector'
import { fromLonLat } from 'ol/proj'
import type VectorSource from 'ol/source/Vector'
import type { ReactNode } from 'react'
import { describe, expect, it } from 'vitest'
import { useMap } from '../../map/MapContext'
import { MapView } from '../../map/MapView'
import { useCircleDraw } from './useCircleDraw'

function wrapper({ children }: { children: ReactNode }) {
  return <MapView>{children}</MapView>
}

function renderCircleDraw() {
  const rendered = renderHook(() => ({ search: useCircleDraw(), map: useMap() }), { wrapper })
  const map = rendered.result.current.map
  return { ...rendered, map, draw: drawOf(map), circleSource: () => lastVectorSource(map) }
}

function drawOf(map: OlMap): Draw {
  return map
    .getInteractions()
    .getArray()
    .find((interaction) => interaction instanceof Draw) as Draw
}

function lastVectorSource(map: OlMap): VectorSource {
  const layers = map.getLayers().getArray()
  return (layers[layers.length - 1] as VectorLayer<VectorSource>).getSource()!
}

/** 1.2 km of ground at Ponta Negra: Web Mercator stretches it by 1 / cos(5.88°), about 0.5%. */
function pontaNegraCircle(groundMeters = 1200): Circle {
  const stretch = 1 / Math.cos((5.88 * Math.PI) / 180)
  return new Circle(fromLonLat([-35.196, -5.88]), groundMeters * stretch)
}

function dispatch(draw: Draw, type: 'drawstart' | 'drawend' | 'drawabort', circle: Circle) {
  act(() => {
    draw.dispatchEvent(new DrawEvent(type, new Feature(circle)))
  })
}

function tooltip(): HTMLElement | null {
  return document.querySelector('.map-tooltip')
}

describe('useCircleDraw', () => {
  it('starts drawing, without a search', () => {
    const { draw, result } = renderCircleDraw()

    expect(draw.getActive()).toBe(true)
    expect(result.current.search).toBeNull()
  })

  it('shows the ground radius on the border while dragging', () => {
    const { draw, map } = renderCircleDraw()
    const circle = pontaNegraCircle()
    const overlay = map.getOverlays().item(0)

    dispatch(draw, 'drawstart', circle)
    expect(tooltip()).toHaveTextContent('1.20 km')
    expect(overlay.getPosition()).toEqual([
      circle.getCenter()[0] + circle.getRadius(),
      circle.getCenter()[1],
    ])

    act(() => {
      circle.setRadius(circle.getRadius() / 4)
    })
    expect(tooltip()).toHaveTextContent('300 m')
  })

  it('hides the radius when the drag is cancelled', () => {
    const { draw, map } = renderCircleDraw()
    const circle = pontaNegraCircle()
    dispatch(draw, 'drawstart', circle)

    dispatch(draw, 'drawabort', circle)

    expect(map.getOverlays().item(0).getPosition()).toBeUndefined()
    act(() => {
      circle.setRadius(1)
    })
    expect(tooltip()).toHaveTextContent('1.20 km')
  })

  it('returns the search of the finished circle, keeps it on the map and stops drawing', () => {
    const { draw, result, circleSource } = renderCircleDraw()
    const circle = pontaNegraCircle()
    dispatch(draw, 'drawstart', circle)

    dispatch(draw, 'drawend', circle)

    const search = result.current.search!
    expect(search.lon).toBeCloseTo(-35.196, 9)
    expect(search.lat).toBeCloseTo(-5.88, 9)
    expect(Math.abs(search.radiusMeters / 1200 - 1)).toBeLessThan(0.005)
    expect(circleSource().getFeatures()[0].getGeometry()).toBe(circle)
    expect(draw.getActive()).toBe(false)
    expect(tooltip()).toHaveTextContent('1.20 km')
  })

  it('ignores a click without dragging and keeps drawing', () => {
    const { draw, result, map, circleSource } = renderCircleDraw()
    const point = new Circle(fromLonLat([-35.196, -5.88]), 0)
    dispatch(draw, 'drawstart', point)

    dispatch(draw, 'drawend', point)

    expect(result.current.search).toBeNull()
    expect(circleSource().getFeatures()).toHaveLength(0)
    expect(draw.getActive()).toBe(true)
    expect(map.getOverlays().item(0).getPosition()).toBeUndefined()
  })

  it('removes everything it added when unmounted', () => {
    const { map, unmount } = renderCircleDraw()
    const layersBefore = map.getLayers().getLength()

    unmount()

    expect(
      map
        .getInteractions()
        .getArray()
        .some((i) => i instanceof Draw),
    ).toBe(false)
    expect(map.getOverlays().getLength()).toBe(0)
    expect(map.getLayers().getLength()).toBe(layersBefore - 1)
  })
})
