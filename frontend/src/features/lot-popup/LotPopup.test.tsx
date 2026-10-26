import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type OlMap from 'ol/Map'
import MapBrowserEvent from 'ol/MapBrowserEvent'
import type { AtPixelOptions } from 'ol/Map'
import type Layer from 'ol/layer/Layer'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import { act } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { LandLotFeature } from '../../api/types'
import { readLandLotFeatures, type LandLotLayer } from '../../map/landLotLayer'
import { useMap } from '../../map/MapContext'
import { MapView } from '../../map/MapView'
import { landLotFeature } from '../../test/fixtures'
import { LotPopup } from './LotPopup'

const CLICKED_AT = [-3_918_000, -655_000]

// jsdom never renders the map, so OpenLayers keeps every overlay at display: none. The queries
// below pass hidden: true to find the popup anyway

function renderPopup() {
  const layer: LandLotLayer = new VectorLayer({ source: new VectorSource() })
  const maps: OlMap[] = []
  function Capture() {
    maps.push(useMap())
    return null
  }
  const rendered = render(
    <MapView>
      <LotPopup layer={layer} />
      <Capture />
    </MapView>,
  )
  return { ...rendered, map: maps[0], layer }
}

/** Makes the map find this feature under the pointer: jsdom renders no canvas to hit-test. */
function lotUnderPointer(map: OlMap, lot: LandLotFeature | null) {
  const feature = lot ? readLandLotFeatures(lot)[0] : undefined
  return vi
    .spyOn(map, 'forEachFeatureAtPixel')
    .mockImplementation((_pixel, callback) =>
      feature ? callback(feature, null as unknown as Layer, feature.getGeometry()!) : undefined,
    )
}

function mapEvent(map: OlMap, type: string, dragging = false) {
  const event = new MapBrowserEvent(type, map, new PointerEvent(type), dragging)
  event.pixel = [10, 10]
  event.coordinate = CLICKED_AT
  act(() => {
    map.dispatchEvent(event)
  })
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('LotPopup', () => {
  it('shows the details of the clicked lot at the click', () => {
    const { map } = renderPopup()
    lotUnderPointer(
      map,
      landLotFeature({
        properties: {
          ...landLotFeature().properties,
          contact: { name: 'Kaio', phone: '+55 84 99999-0000', email: 'kaio@example.com' },
        },
      }),
    )

    mapEvent(map, 'singleclick')

    const popup = screen.getByRole('dialog', { name: 'Corner lot', hidden: true })
    expect(popup).toHaveTextContent('Available')
    expect(popup).toHaveTextContent('R$250,000.00')
    expect(popup).toHaveTextContent('1.22 ha')
    expect(popup).toHaveTextContent('R$20.41')
    expect(popup).toHaveTextContent('Flat terrain')
    expect(popup).toHaveTextContent('Kaio')
    expect(screen.getByRole('link', { name: '+55 84 99999-0000', hidden: true })).toHaveAttribute(
      'href',
      'tel:+55 84 99999-0000',
    )
    expect(screen.getByRole('link', { name: 'kaio@example.com', hidden: true })).toHaveAttribute(
      'href',
      'mailto:kaio@example.com',
    )
    expect(map.getOverlays().item(0).getPosition()).toEqual(CLICKED_AT)
  })

  it('shows sold lots as sold and leaves out a missing phone', () => {
    const { map } = renderPopup()
    const lot = landLotFeature()
    lotUnderPointer(map, { ...lot, properties: { ...lot.properties, status: 'SOLD' } })

    mapEvent(map, 'singleclick')

    expect(screen.getByRole('dialog', { hidden: true })).toHaveTextContent('Sold')
    expect(screen.getAllByRole('link', { hidden: true })).toHaveLength(1)
  })

  it('looks for lots only in its own layer', () => {
    const { map, layer } = renderPopup()
    const spy = lotUnderPointer(map, null)

    mapEvent(map, 'singleclick')

    const { layerFilter } = spy.mock.calls[0][2] as AtPixelOptions
    expect(layerFilter!(layer)).toBe(true)
    expect(layerFilter!(new VectorLayer())).toBe(false)
  })

  it('closes with the close button', async () => {
    const { map } = renderPopup()
    lotUnderPointer(map, landLotFeature())
    mapEvent(map, 'singleclick')

    await userEvent.click(screen.getByRole('button', { name: 'Close', hidden: true }))

    expect(screen.queryByRole('dialog', { hidden: true })).not.toBeInTheDocument()
    expect(map.getOverlays().item(0).getPosition()).toBeUndefined()
  })

  it('closes when the user clicks outside the lots', () => {
    const { map } = renderPopup()
    const spy = lotUnderPointer(map, landLotFeature())
    mapEvent(map, 'singleclick')

    spy.mockReturnValue(undefined)
    mapEvent(map, 'singleclick')

    expect(screen.queryByRole('dialog', { hidden: true })).not.toBeInTheDocument()
  })

  it('shows a pointer cursor over the lots', () => {
    const { map } = renderPopup()
    const hasFeature = vi.spyOn(map, 'hasFeatureAtPixel').mockReturnValue(true)
    const target = map.getViewport()!

    mapEvent(map, 'pointermove')
    expect(target.style.cursor).toBe('pointer')

    hasFeature.mockReturnValue(false)
    mapEvent(map, 'pointermove')
    expect(target.style.cursor).toBe('')
  })

  it('leaves the cursor alone while the map is dragged', () => {
    const { map } = renderPopup()
    const hasFeature = vi.spyOn(map, 'hasFeatureAtPixel')

    mapEvent(map, 'pointermove', true)

    expect(hasFeature).not.toHaveBeenCalled()
  })

  it('removes its overlay, listeners and cursor when unmounted', () => {
    const { map, unmount } = renderPopup()
    vi.spyOn(map, 'hasFeatureAtPixel').mockReturnValue(true)
    mapEvent(map, 'pointermove')
    const target = map.getViewport()!
    const spy = lotUnderPointer(map, landLotFeature())

    unmount()

    expect(map.getOverlays().getLength()).toBe(0)
    expect(target.style.cursor).toBe('')
    map.dispatchEvent(new MapBrowserEvent('singleclick', map, new PointerEvent('click')))
    expect(spy).not.toHaveBeenCalled()
  })
})
