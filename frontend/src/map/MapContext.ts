import { createContext, useContext } from 'react'
import type OlMap from 'ol/Map'

export const MapContext = createContext<OlMap | null>(null)

/**
 * The OpenLayers map of the closest MapView. MapView renders its children only once the map
 * exists, so inside it this never returns null.
 */
export function useMap(): OlMap {
  const map = useContext(MapContext)
  if (!map) {
    throw new Error('useMap must be used inside <MapView>')
  }
  return map
}
