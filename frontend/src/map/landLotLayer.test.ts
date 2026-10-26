import { http, HttpResponse } from 'msw'
import Feature from 'ol/Feature'
import type Polygon from 'ol/geom/Polygon'
import { get as getProjection, toLonLat, transformExtent } from 'ol/proj'
import { describe, expect, it, vi } from 'vitest'
import { server } from '../test/server'
import { landLotFeature } from '../test/fixtures'
import {
  applyFilters,
  clearConflicts,
  createLandLotLayer,
  createLandLotSource,
  highlightConflicts,
  readLandLotFeatures,
  toApiBbox,
} from './landLotLayer'
import { CONFLICT_PROPERTY } from './landLotStyle'

const NATAL_EXTENT = transformExtent([-35.3, -5.9, -35.1, -5.7], 'EPSG:4326', 'EPSG:3857')
const WEB_MERCATOR = getProjection('EPSG:3857')!

function load(source: ReturnType<typeof createLandLotSource>) {
  source.loadFeatures(NATAL_EXTENT, 10, WEB_MERCATOR)
}

describe('toApiBbox', () => {
  it('converts the map extent to WGS84 degrees', () => {
    const bbox = toApiBbox(NATAL_EXTENT)!

    expect(bbox[0]).toBeCloseTo(-35.3, 9)
    expect(bbox[1]).toBeCloseTo(-5.9, 9)
    expect(bbox[2]).toBeCloseTo(-35.1, 9)
    expect(bbox[3]).toBeCloseTo(-5.7, 9)
  })

  it('clamps an extent that goes around the world to the range the API accepts', () => {
    const wide = transformExtent([-200, -80, 200, 80], 'EPSG:4326', 'EPSG:3857')

    expect(toApiBbox(wide)).toEqual([-180, expect.closeTo(-80, 9), 180, expect.closeTo(80, 9)])
  })

  it('returns null when the extent is entirely in a repeated copy of the world', () => {
    expect(toApiBbox(transformExtent([190, -10, 200, 10], 'EPSG:4326', 'EPSG:3857'))).toBeNull()
  })
})

describe('land lot source', () => {
  it('loads the lots of the visible area in the map projection', async () => {
    let bbox: string | null = null
    server.use(
      http.get('*/api/land-lots', ({ request }) => {
        bbox = new URL(request.url).searchParams.get('bbox')
        return HttpResponse.json({
          type: 'FeatureCollection',
          features: [landLotFeature()],
          truncated: false,
        })
      }),
    )
    const source = createLandLotSource()

    load(source)

    await vi.waitFor(() => expect(source.getFeatures()).toHaveLength(1))
    expect(bbox!.split(',').map(Number)).toEqual([
      expect.closeTo(-35.3, 9),
      expect.closeTo(-5.9, 9),
      expect.closeTo(-35.1, 9),
      expect.closeTo(-5.7, 9),
    ])
    const lot = source.getFeatureById(landLotFeature().id)!
    expect(lot.get('title')).toBe('Corner lot')
    const [lon, lat] = toLonLat(lot.getGeometry()!.getCoordinates()[0][0])
    expect(lon).toBeCloseTo(-35.196, 9)
    expect(lat).toBeCloseTo(-5.88, 9)
  })

  it('does not ask again for an area it already loaded', async () => {
    const requests = countRequests({ truncated: false })
    const source = createLandLotSource()

    load(source)
    await vi.waitFor(() => expect(source.getFeatures()).toHaveLength(1))
    load(source)

    expect(requests()).toBe(1)
  })

  it('asks again for an area whose answer was truncated, so zooming in loads the rest', async () => {
    const requests = countRequests({ truncated: true })
    const source = createLandLotSource()

    load(source)
    await vi.waitFor(() => expect(source.getFeatures()).toHaveLength(1))
    load(source)

    await vi.waitFor(() => expect(requests()).toBe(2))
  })

  it('asks again for an area that failed to load', async () => {
    let requests = 0
    server.use(
      http.get('*/api/land-lots', () => {
        requests += 1
        return new HttpResponse(null, { status: 503 })
      }),
    )
    const source = createLandLotSource()
    const failed = vi.fn()
    source.on('featuresloaderror', failed)

    load(source)
    await vi.waitFor(() => expect(failed).toHaveBeenCalled())
    load(source)

    await vi.waitFor(() => expect(requests).toBe(2))
  })

  it('skips the request when nothing of the extent is inside the world', () => {
    const source = createLandLotSource()
    const loaded = vi.fn()
    source.on('featuresloadend', loaded)

    source.loadFeatures(
      transformExtent([190, -10, 200, 10], 'EPSG:4326', 'EPSG:3857'),
      10,
      WEB_MERCATOR,
    )

    expect(loaded).toHaveBeenCalled()
  })

  it('draws the lots with the lot style', () => {
    expect(createLandLotLayer().getStyle()).toBeTypeOf('function')
  })
})

describe('conflict highlight', () => {
  it('marks only the listed lots and clears previous marks', () => {
    const source = createLandLotSource()
    const [first] = readLandLotFeatures(landLotFeature({ id: 'first' }))
    const [second] = readLandLotFeatures(landLotFeature({ id: 'second' }))
    source.addFeatures([first, second])

    highlightConflicts(source, ['first', 'not-loaded'])
    expect(first.get(CONFLICT_PROPERTY)).toBe(true)
    expect(second.get(CONFLICT_PROPERTY)).toBeUndefined()

    highlightConflicts(source, ['second'])
    expect(first.get(CONFLICT_PROPERTY)).toBeUndefined()
    expect(second.get(CONFLICT_PROPERTY)).toBe(true)

    clearConflicts(source)
    expect(second.get(CONFLICT_PROPERTY)).toBeUndefined()
  })

  it('leaves unmarked lots untouched', () => {
    const source = createLandLotSource()
    const lot = new Feature<Polygon>()
    const changed = vi.fn()
    source.addFeature(lot)
    lot.on('propertychange', changed)

    clearConflicts(source)

    expect(changed).not.toHaveBeenCalled()
  })
})

describe('filters of the land lot source', () => {
  function captureParams(): URLSearchParams[] {
    const received: URLSearchParams[] = []
    server.use(
      http.get('*/api/land-lots', ({ request }) => {
        received.push(new URL(request.url).searchParams)
        return HttpResponse.json({
          type: 'FeatureCollection',
          features: [landLotFeature()],
          truncated: false,
        })
      }),
    )
    return received
  }

  it('sends the filters it was created with', async () => {
    const received = captureParams()
    const source = createLandLotSource({ status: 'SOLD' })

    load(source)

    await vi.waitFor(() => expect(received).toHaveLength(1))
    expect(received[0].get('status')).toBe('SOLD')
  })

  it('drops the loaded lots and loads the area again with new filters', async () => {
    const received = captureParams()
    const source = createLandLotSource()
    load(source)
    await vi.waitFor(() => expect(source.getFeatures()).toHaveLength(1))

    applyFilters(source, { maxPrice: 100 })

    expect(source.getFeatures()).toHaveLength(0)
    // OpenLayers loads the visible area again on the next render; here the test asks for it
    load(source)
    await vi.waitFor(() => expect(received).toHaveLength(2))
    expect(received[1].get('maxPrice')).toBe('100')
  })

  it('keeps the loaded lots when the same filters are applied again', async () => {
    const filters = { minArea: 100 }
    const received = captureParams()
    const source = createLandLotSource(filters)
    load(source)
    await vi.waitFor(() => expect(source.getFeatures()).toHaveLength(1))

    applyFilters(source, filters)
    load(source)

    expect(source.getFeatures()).toHaveLength(1)
    expect(received).toHaveLength(1)
  })

  // Otherwise an unfiltered answer arriving late would put back the lots the filter removed
  it('ignores an answer for filters that changed while it was loading', async () => {
    let answer: (() => void) | null = null
    server.use(
      http.get(
        '*/api/land-lots',
        () =>
          new Promise<Response>((resolve) => {
            answer = () =>
              resolve(
                HttpResponse.json({
                  type: 'FeatureCollection',
                  features: [landLotFeature()],
                  truncated: false,
                }),
              )
          }),
        { once: true },
      ),
    )
    const source = createLandLotSource()
    load(source)
    await vi.waitFor(() => expect(answer).not.toBeNull())

    applyFilters(source, { status: 'SOLD' })
    answer!()

    await new Promise((resolve) => setTimeout(resolve, 20))
    expect(source.getFeatures()).toHaveLength(0)
  })
})

function countRequests({ truncated }: { truncated: boolean }): () => number {
  let requests = 0
  server.use(
    http.get('*/api/land-lots', () => {
      requests += 1
      return HttpResponse.json({
        type: 'FeatureCollection',
        features: [landLotFeature()],
        truncated,
      })
    }),
  )
  return () => requests
}
