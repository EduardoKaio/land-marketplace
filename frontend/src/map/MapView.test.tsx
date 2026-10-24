import { StrictMode } from 'react'
import { render, renderHook, screen } from '@testing-library/react'
import type OlMap from 'ol/Map'
import { toLonLat } from 'ol/proj'
import { describe, expect, it } from 'vitest'
import { useMap } from './MapContext'
import { MapView } from './MapView'

/** Records every map instance its render receives. */
function MapProbe({ seen }: { seen: OlMap[] }) {
  seen.push(useMap())
  return <p>map ready</p>
}

describe('MapView', () => {
  it('gives its children the map through useMap', () => {
    const seen: OlMap[] = []

    render(
      <MapView>
        <MapProbe seen={seen} />
      </MapView>,
    )

    expect(screen.getByText('map ready')).toBeInTheDocument()
    expect(seen[0].getTarget()).toBe(screen.getByTestId('map-target'))
  })

  it('creates a single map under StrictMode', () => {
    const seen: OlMap[] = []

    const { container } = render(
      <StrictMode>
        <MapView>
          <MapProbe seen={seen} />
        </MapView>
      </StrictMode>,
    )

    // StrictMode renders twice and runs the effect twice; both must see the same instance
    expect(seen.length).toBeGreaterThan(1)
    expect(new Set(seen).size).toBe(1)
    expect(container.querySelectorAll('.ol-viewport')).toHaveLength(1)
    expect(seen[0].getTarget()).toBe(screen.getByTestId('map-target'))
  })

  it('keeps the same map when the parent re-renders', () => {
    const seen: OlMap[] = []
    const tree = (label: string) => (
      <MapView>
        <MapProbe seen={seen} />
        <span>{label}</span>
      </MapView>
    )
    const { rerender } = render(tree('first'))

    rerender(tree('second'))

    expect(screen.getByText('second')).toBeInTheDocument()
    expect(new Set(seen).size).toBe(1)
  })

  it('detaches the map from the page on unmount', () => {
    const seen: OlMap[] = []
    const { container, unmount } = render(
      <MapView>
        <MapProbe seen={seen} />
      </MapView>,
    )

    unmount()

    expect(seen[0].getTarget()).toBeUndefined()
    expect(container.querySelector('.ol-viewport')).toBeNull()
  })

  it('opens on the given center and zoom', () => {
    const seen: OlMap[] = []

    render(
      <MapView initialView={{ center: [-46.63, -23.55], zoom: 9 }}>
        <MapProbe seen={seen} />
      </MapView>,
    )

    const view = seen[0].getView()
    const [lon, lat] = toLonLat(view.getCenter()!)
    expect(lon).toBeCloseTo(-46.63, 6)
    expect(lat).toBeCloseTo(-23.55, 6)
    expect(view.getZoom()).toBe(9)
  })

  it('opens on Natal by default', () => {
    const seen: OlMap[] = []

    render(
      <MapView>
        <MapProbe seen={seen} />
      </MapView>,
    )

    const [lon, lat] = toLonLat(seen[0].getView().getCenter()!)
    expect(lon).toBeCloseTo(-35.2094, 6)
    expect(lat).toBeCloseTo(-5.7945, 6)
  })
})

describe('useMap', () => {
  it('fails clearly outside a MapView', () => {
    expect(() => renderHook(() => useMap())).toThrow('useMap must be used inside <MapView>')
  })
})
