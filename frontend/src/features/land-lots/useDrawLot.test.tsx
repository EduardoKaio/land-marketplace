import { act, renderHook } from '@testing-library/react'
import Feature from 'ol/Feature'
import type OlMap from 'ol/Map'
import Polygon from 'ol/geom/Polygon'
import Draw, { DrawEvent } from 'ol/interaction/Draw'
import Modify from 'ol/interaction/Modify'
import Snap from 'ol/interaction/Snap'
import VectorSource from 'ol/source/Vector'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useMap } from '../../map/MapContext'
import { MapView } from '../../map/MapView'
import { pontaNegraPolygon } from '../../test/fixtures'
import { useDrawLot } from './useDrawLot'

function wrapper({ children }: { children: ReactNode }) {
  return <MapView>{children}</MapView>
}

function renderDrawLot() {
  const snapSource = new VectorSource<Feature<Polygon>>()
  const rendered = renderHook(() => ({ drawLot: useDrawLot(snapSource), map: useMap() }), {
    wrapper,
  })
  const map = rendered.result.current.map
  return { ...rendered, map, snapSource, draw: interactionOf(map, Draw) }
}

function interactionOf<T>(map: OlMap, type: new (...args: never[]) => T): T {
  return map
    .getInteractions()
    .getArray()
    .find((interaction) => interaction instanceof type) as T
}

/** What Draw does when the user double-clicks, without needing a canvas or pointer events. */
function finishDrawing(draw: Draw, geometry: Polygon) {
  act(() => {
    draw.dispatchEvent(new DrawEvent('drawend', new Feature(geometry)))
  })
}

function tooltip(): HTMLElement | null {
  return document.querySelector('.area-tooltip')
}

function relativeDifference(actual: number, expected: number): number {
  return Math.abs(actual / expected - 1)
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('useDrawLot', () => {
  it('adds Draw, Modify and Snap, with Snap last so it adjusts the pointer first', () => {
    const { map } = renderDrawLot()

    const lastThree = map.getInteractions().getArray().slice(-3)

    expect(lastThree[0]).toBeInstanceOf(Draw)
    expect(lastThree[1]).toBeInstanceOf(Modify)
    expect(lastThree[2]).toBeInstanceOf(Snap)
  })

  it('starts without a draft', () => {
    const { result, draw } = renderDrawLot()

    expect(result.current.drawLot.draft).toBeNull()
    expect(result.current.drawLot.areaM2).toBeNull()
    expect(draw.getActive()).toBe(true)
  })

  it('keeps the finished polygon and its geodesic area, and stops drawing more', () => {
    const { result, draw } = renderDrawLot()
    const polygon = pontaNegraPolygon()

    finishDrawing(draw, polygon)

    expect(result.current.drawLot.draft).toBe(polygon)
    // PostGIS measures 12,246 m² on the WGS84 spheroid; ol/sphere uses a sphere and gets about
    // 0.4% more, fine for a live estimate (the saved lot shows the server value)
    expect(relativeDifference(result.current.drawLot.areaM2!, 12_246)).toBeLessThan(0.01)
    expect(draw.getActive()).toBe(false)
    expect(tooltip()).toHaveTextContent('1.23 ha')
  })

  it('updates the area while the user moves a vertex', () => {
    const { result, draw } = renderDrawLot()
    const polygon = pontaNegraPolygon()
    finishDrawing(draw, polygon)

    const before = result.current.drawLot.areaM2!

    act(() => {
      polygon.scale(2)
    })

    expect(relativeDifference(result.current.drawLot.areaM2!, 4 * before)).toBeLessThan(0.001)
  })

  it('discards the draft and draws again after reset', () => {
    const { result, draw } = renderDrawLot()
    finishDrawing(draw, pontaNegraPolygon())

    act(() => result.current.drawLot.reset())

    expect(result.current.drawLot.draft).toBeNull()
    expect(result.current.drawLot.areaM2).toBeNull()
    expect(draw.getActive()).toBe(true)
  })

  it('shows the live area while sketching and hides it when the sketch is cancelled', () => {
    const { draw, map } = renderDrawLot()
    const sketch = pontaNegraPolygon()
    const overlay = map.getOverlays().item(0)

    act(() => {
      draw.dispatchEvent(new DrawEvent('drawstart', new Feature(sketch)))
    })
    expect(tooltip()).toHaveTextContent('1.23 ha')
    expect(overlay.getPosition()).toBeDefined()

    act(() => {
      sketch.scale(2)
    })
    expect(tooltip()).toHaveTextContent('4.92 ha')

    act(() => {
      draw.dispatchEvent(new DrawEvent('drawabort', new Feature(sketch)))
    })
    expect(overlay.getPosition()).toBeUndefined()

    act(() => {
      sketch.scale(2)
    })
    // No longer following the cancelled sketch
    expect(tooltip()).toHaveTextContent('4.92 ha')
  })

  // Right after the first click the sketch is a polygon whose vertices are all the same point
  it('does not place the tooltip before the sketch has an area', () => {
    const { draw, map } = renderDrawLot()
    const point = pontaNegraPolygon().getCoordinates()[0][0]

    act(() => {
      draw.dispatchEvent(
        new DrawEvent('drawstart', new Feature(new Polygon([[point, point, point, point]]))),
      )
    })

    expect(map.getOverlays().item(0).getPosition()).toBeUndefined()
    expect(tooltip()).toHaveTextContent('0 m²')
  })

  it('cancels the sketch on Escape and removes the last vertex on Backspace', () => {
    const abort = vi.spyOn(Draw.prototype, 'abortDrawing')
    const undo = vi.spyOn(Draw.prototype, 'removeLastPoint')
    renderDrawLot()

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Backspace' }))
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }))

    expect(abort).toHaveBeenCalledTimes(1)
    expect(undo).toHaveBeenCalledTimes(1)
  })

  it('ignores Backspace typed into a form field', () => {
    const undo = vi.spyOn(Draw.prototype, 'removeLastPoint')
    renderDrawLot()
    const input = document.body.appendChild(document.createElement('input'))

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Backspace', bubbles: true }))

    expect(undo).not.toHaveBeenCalled()
    input.remove()
  })

  it('removes everything it added when unmounted', () => {
    const { map, unmount } = renderDrawLot()
    const layersBefore = map.getLayers().getLength()
    const undo = vi.spyOn(Draw.prototype, 'removeLastPoint')

    unmount()

    expect(
      map
        .getInteractions()
        .getArray()
        .some((i) => i instanceof Draw || i instanceof Modify || i instanceof Snap),
    ).toBe(false)
    expect(map.getOverlays().getLength()).toBe(0)
    expect(map.getLayers().getLength()).toBe(layersBefore - 1)
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Backspace' }))
    expect(undo).not.toHaveBeenCalled()
  })
})
