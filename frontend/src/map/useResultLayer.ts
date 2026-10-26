import { useEffect, useState } from 'react'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import type { LandLotFeatureCollection } from '../api/types'
import { readLandLotFeatures, type LandLotLayer, type LandLotSource } from './landLotLayer'
import { landLotStyle } from './landLotStyle'
import { useMap } from './MapContext'

function createResultLayer(): LandLotLayer {
  const source: LandLotSource = new VectorSource()
  // zIndex 1 keeps the lots above the search circle, which is added to the map later
  return new VectorLayer({ source, style: landLotStyle, zIndex: 1 })
}

/**
 * Shows the lots of a search in their own layer. While there are results, the explore layer is
 * hidden so only the results are on the map; unmounting the caller shows it again.
 */
export function useResultLayer(
  exploreLayer: LandLotLayer,
  results: LandLotFeatureCollection | undefined,
): LandLotLayer {
  const map = useMap()
  const [layer] = useState(createResultLayer)

  useEffect(() => {
    map.addLayer(layer)
    return () => {
      map.removeLayer(layer)
    }
  }, [map, layer])

  useEffect(() => {
    if (!results) {
      return
    }
    const source = layer.getSource()!
    source.addFeatures(readLandLotFeatures(results))
    exploreLayer.setVisible(false)
    return () => {
      source.clear()
      exploreLayer.setVisible(true)
    }
  }, [layer, exploreLayer, results])

  return layer
}
