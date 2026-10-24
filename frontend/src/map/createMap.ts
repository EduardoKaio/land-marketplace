import OlMap from 'ol/Map'
import View from 'ol/View'
import { fromLonLat } from 'ol/proj'
import { createBaseLayers } from './baseLayers'
import type { MapStartView } from './config'

/**
 * Builds the map without attaching it to the page. The view uses EPSG:3857 (the OpenLayers
 * default), so the WGS84 start center is converted here.
 */
export function createMap(startView: MapStartView): OlMap {
  return new OlMap({
    layers: createBaseLayers(),
    view: new View({ center: fromLonLat(startView.center), zoom: startView.zoom }),
  })
}
