import { useEffect, useState } from 'react'
import type Feature from 'ol/Feature'
import Overlay from 'ol/Overlay'
import type Circle from 'ol/geom/Circle'
import Draw from 'ol/interaction/Draw'
import VectorLayer from 'ol/layer/Vector'
import { unByKey } from 'ol/Observable'
import VectorSource from 'ol/source/Vector'
import Fill from 'ol/style/Fill'
import Stroke from 'ol/style/Stroke'
import Style from 'ol/style/Style'
import { circleEdge, getCircleSearchParams, type CircleSearchParams } from '../../map/circleSearch'
import { useMap } from '../../map/MapContext'
import { formatDistance } from '../../shared/format'

const CIRCLE_STYLE = new Style({
  fill: new Fill({ color: '#1565c014' }),
  stroke: new Stroke({ color: '#1565c0', width: 2, lineDash: [6, 4] }),
})

/**
 * Search mode: press, drag and release draws one circle, with its ground radius following the
 * border while dragging. Returns the search of the finished circle, or null until there is one.
 * Every map object is added in the effect and removed in its cleanup, so leaving the mode removes
 * the circle too.
 */
export function useCircleDraw(): CircleSearchParams | null {
  const map = useMap()
  const [search, setSearch] = useState<CircleSearchParams | null>(null)

  useEffect(() => {
    // Draw gets no source: the circle is added by hand in drawend, only when it has a radius
    const circleSource = new VectorSource<Feature<Circle>>()
    const circleLayer = new VectorLayer({ source: circleSource, style: CIRCLE_STYLE })
    // freehand turns the gesture into press, drag and release instead of click, move and click
    const draw = new Draw({ type: 'Circle', freehand: true, style: CIRCLE_STYLE })

    // Written straight to the DOM, as in draw-lot mode: it changes on every pointer move
    const tooltipElement = document.createElement('div')
    tooltipElement.className = 'map-tooltip'
    const tooltip = new Overlay({
      element: tooltipElement,
      positioning: 'center-left',
      offset: [8, 0],
      stopEvent: false,
    })

    let stopFollowing = () => {}
    const follow = (circle: Circle) => {
      const update = () => {
        tooltipElement.textContent = formatDistance(getCircleSearchParams(circle).radiusMeters)
        tooltip.setPosition(circleEdge(circle))
      }
      const key = circle.on('change', update)
      stopFollowing = () => unByKey(key)
      update()
    }
    const hideTooltip = () => {
      stopFollowing()
      tooltip.setPosition(undefined)
    }

    draw.on('drawstart', (event) => follow(event.feature.getGeometry() as Circle))
    draw.on('drawabort', hideTooltip)
    draw.on('drawend', (event) => {
      stopFollowing()
      const params = getCircleSearchParams(event.feature.getGeometry() as Circle)
      // A click without dragging finishes a circle of radius zero: not a search, keep drawing
      if (params.radiusMeters === 0) {
        tooltip.setPosition(undefined)
        return
      }
      circleSource.addFeature(event.feature as Feature<Circle>)
      // With freehand on, dragging draws instead of panning: switching it off lets the user move
      // around the results. The tooltip stays, labeling the circle with its radius
      draw.setActive(false)
      setSearch(params)
    })

    map.addLayer(circleLayer)
    map.addOverlay(tooltip)
    map.addInteraction(draw)

    return () => {
      stopFollowing()
      map.removeInteraction(draw)
      map.removeOverlay(tooltip)
      map.removeLayer(circleLayer)
    }
  }, [map])

  return search
}
