import type OlMap from 'ol/Map'
import TileLayer from 'ol/layer/Tile'
import OSM from 'ol/source/OSM'
import XYZ from 'ol/source/XYZ'

export type BaseLayerKey = 'osm' | 'satellite'

/** Layer property that marks a base layer and says which one it is. */
export const BASE_LAYER_PROPERTY = 'baseLayer'

/** Credit required by the imagery provider's terms of use. */
export const SATELLITE_ATTRIBUTION =
  'Tiles &copy; Esri, Source: Esri, Maxar, Earthstar Geographics, and the GIS User Community'

const SATELLITE_URL =
  'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}'

/** Both base layers, street map visible first. OSM brings its own attribution. */
export function createBaseLayers(): TileLayer[] {
  const osm = new TileLayer({
    source: new OSM(),
    properties: { [BASE_LAYER_PROPERTY]: 'osm' },
  })
  const satellite = new TileLayer({
    source: new XYZ({ url: SATELLITE_URL, attributions: SATELLITE_ATTRIBUTION, maxZoom: 19 }),
    visible: false,
    properties: { [BASE_LAYER_PROPERTY]: 'satellite' },
  })
  return [osm, satellite]
}

/**
 * Shows only the chosen base layer. Toggling visibility is cheaper than removing and adding
 * layers, and each layer keeps its loaded tiles for when the user switches back.
 */
export function setVisibleBaseLayer(map: OlMap, key: BaseLayerKey): void {
  map.getLayers().forEach((layer) => {
    const layerKey = layer.get(BASE_LAYER_PROPERTY) as BaseLayerKey | undefined
    if (layerKey) {
      layer.setVisible(layerKey === key)
    }
  })
}
