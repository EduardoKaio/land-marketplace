import { useCallback, useEffect, useState } from 'react'
import type MapBrowserEvent from 'ol/MapBrowserEvent'
import Overlay from 'ol/Overlay'
import type Layer from 'ol/layer/Layer'
import type { LandLotProperties } from '../../api/types'
import type { LandLotLayer } from '../../map/landLotLayer'
import { useMap } from '../../map/MapContext'

export interface LotPopup {
  /** Properties of the clicked lot, or null while the popup is closed. */
  lot: LandLotProperties | null
  /** Element the overlay positions on the map; the popup content is rendered into it. */
  container: HTMLElement
  close: () => void
}

function createPopupOverlay(): Overlay {
  return new Overlay({
    element: document.createElement('div'),
    positioning: 'bottom-center',
    offset: [0, -12],
  })
}

/**
 * Clicking a lot of the layer opens a popup at the click; clicking elsewhere on the map closes
 * it. The pointer turns into a hand over the lots, so users know they can click them.
 *
 * The overlay owns its element and moves it into the map. React never renders that element
 * itself, only portals the content into it, so OpenLayers moving it around cannot confuse React.
 */
export function useLotPopup(layer: LandLotLayer): LotPopup {
  const map = useMap()
  // Building the overlay has no side effect until it is added to the map
  const [overlay] = useState(createPopupOverlay)
  const [lot, setLot] = useState<LandLotProperties | null>(null)

  const close = useCallback(() => {
    overlay.setPosition(undefined)
    setLot(null)
  }, [overlay])

  useEffect(() => {
    const layerFilter = (candidate: Layer) => candidate === layer

    const onClick = (event: MapBrowserEvent) => {
      const feature = map.forEachFeatureAtPixel(event.pixel, (found) => found, { layerFilter })
      if (!feature) {
        close()
        return
      }
      // Lots come from readLandLotFeatures, which copies the API properties onto the feature
      setLot(feature.getProperties() as LandLotProperties)
      overlay.setPosition(event.coordinate)
    }

    const onPointerMove = (event: MapBrowserEvent) => {
      if (event.dragging) {
        return
      }
      const overLot = map.hasFeatureAtPixel(event.pixel, { layerFilter })
      setCursor(overLot ? 'pointer' : '')
    }
    // The viewport, not the target element: it belongs to the map for its whole life, while the
    // target is detached by MapView's cleanup, which runs before this one. It is only missing
    // for maps built without a DOM, hence the check
    const setCursor = (cursor: string) => {
      const viewport = map.getViewport()
      if (viewport) {
        viewport.style.cursor = cursor
      }
    }

    map.addOverlay(overlay)
    map.on('singleclick', onClick)
    map.on('pointermove', onPointerMove)

    return () => {
      map.un('singleclick', onClick)
      map.un('pointermove', onPointerMove)
      map.removeOverlay(overlay)
      setCursor('')
    }
  }, [map, layer, overlay, close])

  // Pans the map when the popup opened partly outside the view. Not the overlay's autoPan option:
  // it pans when the position changes, before React has rendered the content, so it would measure
  // an empty element
  useEffect(() => {
    if (lot) {
      overlay.panIntoView({ animation: { duration: 250 } })
    }
  }, [overlay, lot])

  return { lot, container: overlay.getElement()!, close }
}
