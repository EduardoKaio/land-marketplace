import { useEffect, useRef, useState, type ReactNode } from 'react'
import type OlMap from 'ol/Map'
import 'ol/ol.css'
import { readStartView, type MapStartView } from './config'
import { createMap } from './createMap'
import { MapContext } from './MapContext'
import './map.css'

interface MapViewProps {
  /** Only read when the map is created; later changes do not move the map. */
  initialView?: MapStartView
  /** Map controls and layers; rendered once the map exists, so they can call useMap(). */
  children?: ReactNode
}

/**
 * Owns the OpenLayers map. The map is created once, on the first mount, and kept in a ref; React
 * renders never recreate it. Effects only attach it to the page and detach it on unmount.
 */
export function MapView({ initialView, children }: MapViewProps) {
  const targetRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<OlMap | null>(null)
  const initialViewRef = useRef(initialView)
  const [map, setMap] = useState<OlMap | null>(null)

  useEffect(() => {
    // StrictMode mounts, unmounts and mounts again in development; the ref survives that, so the
    // second mount reuses the same map instead of building a second one
    mapRef.current ??= createMap(initialViewRef.current ?? readStartView())
    const instance = mapRef.current
    // React fills the ref before running effects, so the element always exists here
    instance.setTarget(targetRef.current!)
    // Publishes the instance to the children through the context once it is attached
    setMap(instance)
    return () => {
      // Removes the map viewport and its DOM listeners from the element
      instance.setTarget(undefined)
    }
  }, [])

  return (
    <div className="map-view">
      <div ref={targetRef} className="map-view__target" data-testid="map-target" />
      {map && <MapContext.Provider value={map}>{children}</MapContext.Provider>}
    </div>
  )
}
