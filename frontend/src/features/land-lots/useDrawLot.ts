import { useCallback, useEffect, useRef, useState } from 'react'
import type Feature from 'ol/Feature'
import Overlay from 'ol/Overlay'
import type Polygon from 'ol/geom/Polygon'
import Draw from 'ol/interaction/Draw'
import Modify from 'ol/interaction/Modify'
import Snap from 'ol/interaction/Snap'
import VectorLayer from 'ol/layer/Vector'
import { unByKey } from 'ol/Observable'
import VectorSource from 'ol/source/Vector'
import { getArea } from 'ol/sphere'
import Stroke from 'ol/style/Stroke'
import Style from 'ol/style/Style'
import Fill from 'ol/style/Fill'
import { useMap } from '../../map/MapContext'
import { formatArea } from '../../shared/format'
import { drawKeyAction } from './drawKeys'

export interface DrawLot {
  /** The finished polygon in the map projection, edited in place while the user moves vertices. */
  draft: Polygon | null
  /** Geodesic area of the draft in square meters. */
  areaM2: number | null
  /** Discards the draft and lets the user draw again. */
  reset: () => void
}

const DRAFT_STYLE = new Style({
  fill: new Fill({ color: '#1565c033' }),
  stroke: new Stroke({ color: '#1565c0', width: 2 }),
})

/**
 * Draw-lot mode: draws one polygon, lets the user adjust it and snaps to existing lots. Every map
 * object is added in the effect and removed in its cleanup, so leaving the mode (unmounting the
 * caller) leaves nothing behind on the map.
 */
export function useDrawLot(snapSource: VectorSource<Feature<Polygon>>): DrawLot {
  const map = useMap()
  const [draft, setDraft] = useState<Polygon | null>(null)
  const [areaM2, setAreaM2] = useState<number | null>(null)
  const resetRef = useRef<() => void>(() => {})

  useEffect(() => {
    const draftSource = new VectorSource<Feature<Polygon>>()
    const draftLayer = new VectorLayer({ source: draftSource, style: DRAFT_STYLE })
    const draw = new Draw({ source: draftSource, type: 'Polygon' })
    const modify = new Modify({ source: draftSource })
    const snap = new Snap({ source: snapSource })

    // Live area label. Written straight to the DOM: going through React state would re-render on
    // every mouse move while sketching
    const tooltipElement = document.createElement('div')
    tooltipElement.className = 'area-tooltip'
    const tooltip = new Overlay({
      element: tooltipElement,
      positioning: 'center-center',
      stopEvent: false,
    })

    let stopFollowing = () => {}
    const follow = (geometry: Polygon, onArea?: (area: number) => void) => {
      stopFollowing()
      const update = () => {
        const area = getArea(geometry)
        tooltipElement.textContent = formatArea(area)
        // A polygon with fewer than three distinct vertices has no inside point to anchor to
        tooltip.setPosition(area > 0 ? geometry.getInteriorPoint().getCoordinates() : undefined)
        onArea?.(area)
      }
      const key = geometry.on('change', update)
      stopFollowing = () => unByKey(key)
      update()
    }
    const hideTooltip = () => {
      stopFollowing()
      tooltip.setPosition(undefined)
    }

    draw.on('drawstart', (event) => follow(event.feature.getGeometry() as Polygon))
    draw.on('drawabort', hideTooltip)
    draw.on('drawend', (event) => {
      // Draw adds the feature to draftSource right after this event, so it is taken from the event
      const geometry = event.feature.getGeometry() as Polygon
      draw.setActive(false)
      setDraft(geometry)
      follow(geometry, setAreaM2)
    })

    const onKeyDown = (event: KeyboardEvent) => {
      const action = drawKeyAction(event)
      if (action === 'abort') {
        draw.abortDrawing()
      } else if (action === 'undo') {
        draw.removeLastPoint()
      }
    }

    resetRef.current = () => {
      hideTooltip()
      draftSource.clear()
      draw.setActive(true)
      setDraft(null)
      setAreaM2(null)
    }

    map.addLayer(draftLayer)
    map.addOverlay(tooltip)
    map.addInteraction(draw)
    map.addInteraction(modify)
    // Last on purpose: OpenLayers hands events to the most recently added interaction first, so
    // Snap moves the pointer onto the nearby vertex or edge before Draw and Modify use it
    map.addInteraction(snap)
    document.addEventListener('keydown', onKeyDown)

    return () => {
      document.removeEventListener('keydown', onKeyDown)
      stopFollowing()
      map.removeInteraction(snap)
      map.removeInteraction(modify)
      map.removeInteraction(draw)
      map.removeOverlay(tooltip)
      map.removeLayer(draftLayer)
      resetRef.current = () => {}
    }
  }, [map, snapSource])

  const reset = useCallback(() => resetRef.current(), [])

  return { draft, areaM2, reset }
}
