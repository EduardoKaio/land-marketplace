import { useEffect, useState } from 'react'
import { createLandLotLayer } from './landLotLayer'
import { useMap } from './MapContext'

/**
 * Adds the lot layer to the map while the calling component is mounted. Creating the layer has no
 * side effect until it is added, so StrictMode's discarded extra instance costs nothing.
 */
export function useLandLotLayer() {
  const map = useMap()
  const [layer] = useState(createLandLotLayer)

  useEffect(() => {
    map.addLayer(layer)
    return () => {
      map.removeLayer(layer)
    }
  }, [map, layer])

  return layer
}
