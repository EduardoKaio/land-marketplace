import OlMap from 'ol/Map'
import type TileLayer from 'ol/layer/Tile'
import XYZ from 'ol/source/XYZ'
import { describe, expect, it } from 'vitest'
import {
  BASE_LAYER_PROPERTY,
  SATELLITE_ATTRIBUTION,
  createBaseLayers,
  setVisibleBaseLayer,
} from './baseLayers'

function byKey(layers: TileLayer[], key: string): TileLayer {
  return layers.find((layer) => layer.get(BASE_LAYER_PROPERTY) === key)!
}

describe('base layers', () => {
  it('creates the street map visible and the satellite hidden', () => {
    const layers = createBaseLayers()

    expect(byKey(layers, 'osm').getVisible()).toBe(true)
    expect(byKey(layers, 'satellite').getVisible()).toBe(false)
  })

  it('credits the satellite imagery provider', () => {
    const source = byKey(createBaseLayers(), 'satellite').getSource() as XYZ

    const attributions = source.getAttributions()?.({} as never)

    expect(attributions).toEqual([SATELLITE_ATTRIBUTION])
  })

  it('shows only the chosen base layer and leaves other layers alone', () => {
    const layers = createBaseLayers()
    const map = new OlMap({ layers })
    const overlay = byKey(createBaseLayers(), 'osm')
    overlay.unset(BASE_LAYER_PROPERTY)
    map.addLayer(overlay)

    setVisibleBaseLayer(map, 'satellite')

    expect(byKey(layers, 'osm').getVisible()).toBe(false)
    expect(byKey(layers, 'satellite').getVisible()).toBe(true)
    expect(overlay.getVisible()).toBe(true)

    setVisibleBaseLayer(map, 'osm')

    expect(byKey(layers, 'osm').getVisible()).toBe(true)
    expect(byKey(layers, 'satellite').getVisible()).toBe(false)
  })
})
