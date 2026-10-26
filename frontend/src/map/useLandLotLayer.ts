import { useEffect, useState } from 'react'
import type { LandLotFilters } from '../api/types'
import { applyFilters, createLandLotLayer } from './landLotLayer'
import { useMap } from './MapContext'

const NO_FILTERS: LandLotFilters = {}

/**
 * Adds the lot layer to the map while the calling component is mounted. Creating the layer has no
 * side effect until it is added, so StrictMode's discarded extra instance costs nothing. New
 * filters reload the visible area (see applyFilters) without recreating the layer.
 */
export function useLandLotLayer(filters: LandLotFilters = NO_FILTERS) {
  const map = useMap()
  const [layer] = useState(() => createLandLotLayer(filters))

  useEffect(() => {
    map.addLayer(layer)
    return () => {
      map.removeLayer(layer)
    }
  }, [map, layer])

  useEffect(() => {
    applyFilters(layer.getSource()!, filters)
  }, [layer, filters])

  return layer
}
