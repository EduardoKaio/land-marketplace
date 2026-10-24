import { useEffect, useState } from 'react'
import { setVisibleBaseLayer, type BaseLayerKey } from './baseLayers'
import { useMap } from './MapContext'

/** The visible base layer as React state, applied to the map whenever it changes. */
export function useBaseLayer(initial: BaseLayerKey = 'osm') {
  const map = useMap()
  const [baseLayer, setBaseLayer] = useState<BaseLayerKey>(initial)

  useEffect(() => {
    setVisibleBaseLayer(map, baseLayer)
  }, [map, baseLayer])

  return [baseLayer, setBaseLayer] as const
}
