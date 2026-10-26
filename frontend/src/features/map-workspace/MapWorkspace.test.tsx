import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import type OlMap from 'ol/Map'
import Draw, { DrawEvent } from 'ol/interaction/Draw'
import Feature from 'ol/Feature'
import { act } from 'react'
import Circle from 'ol/geom/Circle'
import MapBrowserEvent from 'ol/MapBrowserEvent'
import type { AtPixelOptions } from 'ol/Map'
import { fromLonLat } from 'ol/proj'
import { describe, expect, it, vi } from 'vitest'
import type { LandLotFeatureCollection } from '../../api/types'
import { requestJson } from '../../api/http'
import { useMap } from '../../map/MapContext'
import { MapView } from '../../map/MapView'
import { CONFLICT_PROPERTY, landLotStyle } from '../../map/landLotStyle'
import type VectorLayer from 'ol/layer/Vector'
import { landLotFeature, pontaNegraPolygon } from '../../test/fixtures'
import { TestProviders } from '../../test/providers'
import { signIn } from '../../test/session'
import { server } from '../../test/server'
import { MapWorkspace } from './MapWorkspace'
import type { LandLotSource } from '../../map/landLotLayer'
import { readLandLotFeatures } from '../../map/landLotLayer'

function renderWorkspace() {
  const maps: OlMap[] = []
  function Capture() {
    maps.push(useMap())
    return null
  }
  render(
    <TestProviders>
      <MapView>
        <MapWorkspace />
        <Capture />
      </MapView>
    </TestProviders>,
  )
  const map = maps[0]
  const draw = () =>
    map
      .getInteractions()
      .getArray()
      .find((i) => i instanceof Draw) as Draw | undefined
  // Explore and result layers share the lot style; the explore layer is always added first
  const lotLayers = () =>
    map
      .getLayers()
      .getArray()
      .filter(
        (candidate) => (candidate as VectorLayer).getStyle?.() === landLotStyle,
      ) as VectorLayer<LandLotSource>[]
  const lotSource = () => lotLayers()[0].getSource()!
  return { map, draw, lotLayers, lotSource }
}

function drawPolygon(draw: Draw) {
  act(() => {
    draw.dispatchEvent(new DrawEvent('drawend', new Feature(pontaNegraPolygon())))
  })
}

/** What Draw does when the user releases the mouse. 1206 Web Mercator units at Ponta Negra are
 * about 1.2 km of ground. */
function drawCircle(draw: Draw): Circle {
  const circle = new Circle(fromLonLat([-35.196, -5.88]), 1206)
  act(() => {
    draw.dispatchEvent(new DrawEvent('drawend', new Feature(circle)))
  })
  return circle
}

function collection(features = [landLotFeature()], truncated = false): LandLotFeatureCollection {
  return { type: 'FeatureCollection', features, truncated }
}

function serveSearch(response: () => Response): { url: () => URL } {
  let url: URL | null = null
  server.use(
    http.get('*/api/land-lots/search', ({ request }) => {
      url = new URL(request.url)
      return response()
    }),
  )
  return { url: () => url! }
}

describe('MapWorkspace', () => {
  it('adds the drawing interactions only while drawing a lot', async () => {
    signIn()
    const user = userEvent.setup()
    const { draw } = renderWorkspace()
    expect(draw()).toBeUndefined()

    await user.click(screen.getByRole('button', { name: 'New lot' }))
    expect(draw()).toBeDefined()
    expect(screen.getByText(/Click to add vertices/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Explore' }))
    expect(draw()).toBeUndefined()
  })

  it('opens the form when the polygon is finished and adds the published lot to the map', async () => {
    signIn()
    server.use(
      http.post('*/api/land-lots', () => HttpResponse.json(landLotFeature(), { status: 201 })),
    )
    const user = userEvent.setup()
    const { draw, lotSource } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'New lot' }))

    drawPolygon(draw()!)
    await user.type(screen.getByLabelText('Title'), 'Corner lot')
    await user.type(screen.getByLabelText('Total price (BRL)'), '250000')
    await user.type(screen.getByLabelText('Description'), 'Flat terrain')
    await user.type(screen.getByLabelText('Name'), 'Kaio')
    await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
    await user.click(screen.getByRole('button', { name: 'Publish lot' }))

    expect(await screen.findByText('Lot "Corner lot" published')).toBeInTheDocument()
    expect(lotSource().getFeatureById(landLotFeature().id)).not.toBeNull()
    expect(screen.queryByRole('heading', { name: 'New land lot' })).not.toBeInTheDocument()
    expect(draw()!.getActive()).toBe(true)
  })

  it('marks the conflicting lots in red and clears the marks on cancel', async () => {
    signIn()
    server.use(
      http.post('*/api/land-lots', () =>
        HttpResponse.json(
          { status: 409, conflictingLotIds: ['existing'] },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const user = userEvent.setup()
    const { draw, lotSource } = renderWorkspace()
    const [existing] = readLandLotFeatures(landLotFeature({ id: 'existing' }))
    lotSource().addFeature(existing)
    await user.click(screen.getByRole('button', { name: 'New lot' }))

    drawPolygon(draw()!)
    await user.type(screen.getByLabelText('Title'), 'Overlapping lot')
    await user.type(screen.getByLabelText('Total price (BRL)'), '1000')
    await user.type(screen.getByLabelText('Description'), 'Desc')
    await user.type(screen.getByLabelText('Name'), 'Kaio')
    await user.type(screen.getByLabelText('Phone'), '84999998888')
    await user.click(screen.getByRole('button', { name: 'Publish lot' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('overlaps 1 existing lot')
    expect(existing.get(CONFLICT_PROPERTY)).toBe(true)

    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(existing.get(CONFLICT_PROPERTY)).toBeUndefined()
    expect(screen.getByText(/Click to add vertices/)).toBeInTheDocument()
  })

  it('searches the drawn circle and shows only the lots found', async () => {
    const request = serveSearch(() => HttpResponse.json(collection()))
    const user = userEvent.setup()
    const { map, draw, lotLayers } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'Search' }))
    expect(screen.getByText(/Press, drag and release/)).toBeInTheDocument()

    drawCircle(draw()!)

    expect(await screen.findByText('1 lot found')).toBeInTheDocument()
    const params = request.url().searchParams
    expect(Number(params.get('lon'))).toBeCloseTo(-35.196, 6)
    expect(Number(params.get('lat'))).toBeCloseTo(-5.88, 6)
    expect(Math.abs(Number(params.get('radius')) / 1200 - 1)).toBeLessThan(0.005)
    const [exploreLayer, resultLayer] = lotLayers()
    expect(exploreLayer.getVisible()).toBe(false)
    expect(resultLayer.getSource()!.getFeatureById(landLotFeature().id)).not.toBeNull()
    // The circle stays on the map under the results
    const circleLayer = map
      .getLayers()
      .getArray()
      .find((layer) =>
        (layer as VectorLayer)
          .getSource?.()
          ?.getFeatures?.()
          .some((feature: Feature) => feature.getGeometry() instanceof Circle),
      )
    expect(circleLayer).toBeDefined()
  })

  it('opens the popup for the lots found', async () => {
    serveSearch(() => HttpResponse.json(collection()))
    const user = userEvent.setup()
    const { map, draw, lotLayers } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'Search' }))
    drawCircle(draw()!)
    await screen.findByText('1 lot found')
    const resultLayer = lotLayers()[1]
    const [lot] = resultLayer.getSource()!.getFeatures()
    const hitTest = vi
      .spyOn(map, 'forEachFeatureAtPixel')
      .mockImplementation((_pixel, callback) => callback(lot, resultLayer, lot.getGeometry()!))

    act(() => {
      map.dispatchEvent(new MapBrowserEvent('singleclick', map, new PointerEvent('click')))
    })

    // hidden: jsdom never renders the map, so OpenLayers keeps overlays at display: none
    expect(screen.getByRole('dialog', { name: 'Corner lot', hidden: true })).toBeInTheDocument()
    const { layerFilter } = hitTest.mock.calls[0][2] as AtPixelOptions
    expect(layerFilter!(resultLayer)).toBe(true)
    expect(layerFilter!(lotLayers()[0])).toBe(false)
  })

  it('counts several lots and warns when the server capped the answer', async () => {
    const second = landLotFeature({ id: 'second' })
    serveSearch(() => HttpResponse.json(collection([landLotFeature(), second], true)))
    const user = userEvent.setup()
    const { draw } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'Search' }))

    drawCircle(draw()!)

    expect(await screen.findByText('2 lots found')).toBeInTheDocument()
    expect(screen.getByText(/More lots match/)).toBeInTheDocument()
  })

  it('shows a friendly empty state when no lot is in the circle', async () => {
    serveSearch(() => HttpResponse.json(collection([])))
    const user = userEvent.setup()
    const { draw } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'Search' }))

    drawCircle(draw()!)

    expect(await screen.findByText('No lots in this area')).toBeInTheDocument()
    expect(screen.getByText(/Try a larger circle/)).toBeInTheDocument()
  })

  it('shows why the server rejected the search', async () => {
    serveSearch(() =>
      HttpResponse.json(
        {
          status: 400,
          detail: 'The search radius must be at most 50000 meters.',
          maxRadiusMeters: 50000,
        },
        { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
      ),
    )
    const user = userEvent.setup()
    const { draw, lotLayers } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'Search' }))

    drawCircle(draw()!)

    expect(
      await screen.findByText('The search radius must be at most 50000 meters.'),
    ).toBeInTheDocument()
    expect(lotLayers()[0].getVisible()).toBe(true)
  })

  it('shows a generic message when the search fails on the server side', async () => {
    serveSearch(() => new HttpResponse(null, { status: 503 }))
    const user = userEvent.setup()
    const { draw } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'Search' }))

    drawCircle(draw()!)

    expect(await screen.findByText('Could not search lots. Please try again.')).toBeInTheDocument()
  })

  it('clears the search and goes back to exploring', async () => {
    serveSearch(() => HttpResponse.json(collection()))
    const user = userEvent.setup()
    const { map, draw, lotLayers } = renderWorkspace()
    const layersBefore = map.getLayers().getLength()
    await user.click(screen.getByRole('button', { name: 'Search' }))
    drawCircle(draw()!)
    await screen.findByText('1 lot found')

    await user.click(screen.getByRole('button', { name: 'Clear search' }))

    expect(screen.getByRole('button', { name: 'Explore' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.queryByText('1 lot found')).not.toBeInTheDocument()
    expect(draw()).toBeUndefined()
    expect(map.getLayers().getLength()).toBe(layersBefore)
    expect(lotLayers()).toHaveLength(1)
    expect(lotLayers()[0].getVisible()).toBe(true)
  })

  it('asks to log in before drawing a lot, then opens the drawing', async () => {
    server.use(
      http.post('*/api/auth/login', () =>
        HttpResponse.json({
          accessToken: 'token',
          tokenType: 'Bearer',
          expiresIn: 7200,
          user: { id: 'u1', name: 'Kaio', email: 'kaio@example.com' },
        }),
      ),
    )
    const user = userEvent.setup()
    const { draw } = renderWorkspace()

    await user.click(screen.getByRole('button', { name: 'New lot' }))

    const dialog = screen.getByRole('dialog', { name: 'Log in' })
    expect(dialog).toHaveTextContent('Log in to publish a lot.')
    expect(draw()).toBeUndefined()

    await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(within(dialog).getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText(/Click to add vertices/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'New lot' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByText('Kaio')).toBeInTheDocument()
  })

  it('opens the login from the account bar and stays exploring', async () => {
    const user = userEvent.setup()
    renderWorkspace()

    await user.click(screen.getByRole('button', { name: 'Log in' }))
    await user.click(screen.getByRole('button', { name: 'Close' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Explore' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('leaves the drawing when the user logs out', async () => {
    signIn()
    const user = userEvent.setup()
    const { draw } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'New lot' }))
    expect(draw()).toBeDefined()

    await user.click(screen.getByRole('button', { name: 'Log out' }))

    expect(draw()).toBeUndefined()
    expect(screen.getByRole('button', { name: 'Explore' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument()
  })

  it('leaves the drawing when the session expires', async () => {
    signIn()
    server.use(
      http.get('*/api/me/land-lots', () =>
        HttpResponse.json(
          { status: 401 },
          { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const user = userEvent.setup()
    const { draw } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'New lot' }))

    await act(() => requestJson('/api/me/land-lots').catch(() => {}))

    expect(draw()).toBeUndefined()
    expect(screen.getByText('Your session expired. Please log in again.')).toBeInTheDocument()
  })

  it('opens my lots from the account bar, leaving the current tool', async () => {
    signIn()
    server.use(
      http.get('*/api/me/land-lots', () =>
        HttpResponse.json({
          type: 'FeatureCollection',
          features: [landLotFeature()],
          truncated: false,
        }),
      ),
    )
    const user = userEvent.setup()
    renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'Search' }))

    await user.click(screen.getByRole('button', { name: 'My lots' }))

    expect(screen.getByRole('complementary', { name: 'My lots' })).toBeInTheDocument()
    expect(await screen.findByText('Corner lot')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Explore' })).toHaveAttribute('aria-pressed', 'true')

    // Another tool takes the right side back
    await user.click(screen.getByRole('button', { name: 'New lot' }))
    expect(screen.queryByRole('complementary', { name: 'My lots' })).not.toBeInTheDocument()
  })

  it('searches the same circle again when the filters change', async () => {
    const requests: URLSearchParams[] = []
    server.use(
      http.get('*/api/land-lots/search', ({ request }) => {
        const params = new URL(request.url).searchParams
        requests.push(params)
        return HttpResponse.json(params.get('status') === 'SOLD' ? collection([]) : collection())
      }),
    )
    const user = userEvent.setup()
    const { draw } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'Search' }))
    drawCircle(draw()!)
    await screen.findByText('1 lot found')

    await user.click(screen.getByRole('button', { name: 'Filters' }))
    await user.selectOptions(screen.getByLabelText('Status'), 'SOLD')
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    expect(await screen.findByText('No lots match the filters in this area')).toBeInTheDocument()
    expect(requests).toHaveLength(2)
    expect(requests[1].get('lon')).toBe(requests[0].get('lon'))
    expect(requests[1].get('radius')).toBe(requests[0].get('radius'))
    expect(requests[1].get('status')).toBe('SOLD')
    expect(screen.getByRole('button', { name: 'Filters (1)' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Clear filters' }))

    expect(await screen.findByText('1 lot found')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Filters' })).toBeInTheDocument()
  })

  it('reloads the map listing with the filters', async () => {
    const user = userEvent.setup()
    const { lotSource } = renderWorkspace()
    const [existing] = readLandLotFeatures(landLotFeature({ id: 'existing' }))
    lotSource().addFeature(existing)

    await user.click(screen.getByRole('button', { name: 'Filters' }))
    await user.type(screen.getByLabelText('Max price'), '100')
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    // Dropped now; the visible area loads again, filtered, on the next map render
    expect(lotSource().getFeatures()).toHaveLength(0)
    expect(lotSource().get('filters')).toEqual({ maxPrice: 100 })
  })
})
