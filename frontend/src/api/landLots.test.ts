import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/server'
import { ApiError } from './http'
import { createLandLot, getLandLot, listLandLotsInBbox, searchLandLots } from './landLots'
import type { CreateLandLotRequest, LandLotFeature, LandLotFeatureCollection } from './types'

const SQUARE = {
  type: 'Polygon' as const,
  coordinates: [
    [
      [-35.196, -5.88],
      [-35.195, -5.88],
      [-35.195, -5.879],
      [-35.196, -5.879],
      [-35.196, -5.88],
    ] as [number, number][],
  ],
}

const FEATURE: LandLotFeature = {
  type: 'Feature',
  id: '3f0c2a8e-5b1d-4c7a-9e2f-1a6b8d4c0e93',
  geometry: SQUARE,
  properties: {
    title: 'Corner lot',
    price: 250000,
    areaSquareMeters: 12246.04,
    pricePerSquareMeter: 20.41,
    description: 'Flat terrain',
    contact: { name: 'Kaio', phone: null, email: 'kaio@example.com' },
    status: 'AVAILABLE',
    createdAt: '2026-10-24T14:30:00Z',
  },
}

const COLLECTION: LandLotFeatureCollection = {
  type: 'FeatureCollection',
  features: [FEATURE],
  truncated: false,
}

function problem(status: number, body: object) {
  return HttpResponse.json(
    { status, ...body },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

describe('land lot API client', () => {
  it('posts a new lot as JSON and returns the created feature', async () => {
    let received: unknown
    let contentType: string | null = null
    server.use(
      http.post('*/api/land-lots', async ({ request }) => {
        received = await request.json()
        contentType = request.headers.get('Content-Type')
        return HttpResponse.json(FEATURE, { status: 201 })
      }),
    )
    const request: CreateLandLotRequest = {
      title: 'Corner lot',
      price: 250000,
      description: 'Flat terrain',
      contact: { name: 'Kaio', email: 'kaio@example.com' },
      geometry: SQUARE,
    }

    await expect(createLandLot(request)).resolves.toEqual(FEATURE)
    expect(received).toEqual(request)
    expect(contentType).toBe('application/json')
  })

  it('sends the bounding box as minLon,minLat,maxLon,maxLat', async () => {
    let bbox: string | null = null
    server.use(
      http.get('*/api/land-lots', ({ request }) => {
        bbox = new URL(request.url).searchParams.get('bbox')
        return HttpResponse.json(COLLECTION)
      }),
    )

    await expect(listLandLotsInBbox([-35.21, -5.91, -35.19, -5.89])).resolves.toEqual(COLLECTION)
    expect(bbox).toBe('-35.21,-5.91,-35.19,-5.89')
  })

  it('sends the circle as lon, lat and radius', async () => {
    let params: URLSearchParams | null = null
    server.use(
      http.get('*/api/land-lots/search', ({ request }) => {
        params = new URL(request.url).searchParams
        return HttpResponse.json(COLLECTION)
      }),
    )

    await searchLandLots({ lon: -35.2, lat: -5.9, radius: 1500 })

    expect(Object.fromEntries(params!)).toEqual({ lon: '-35.2', lat: '-5.9', radius: '1500' })
  })

  it('gets a lot by id', async () => {
    server.use(
      http.get('*/api/land-lots/:id', ({ params }) =>
        HttpResponse.json({ ...FEATURE, id: params.id }),
      ),
    )

    await expect(getLandLot(FEATURE.id)).resolves.toEqual(FEATURE)
  })

  it('turns an overlap into an ApiError carrying the conflicting lots', async () => {
    server.use(
      http.post('*/api/land-lots', () =>
        problem(409, {
          title: 'Land lot overlaps existing lots',
          detail: 'The submitted polygon overlaps 1 existing land lot.',
          conflictingLotIds: [FEATURE.id],
        }),
      ),
    )

    const error = await createLandLot({} as CreateLandLotRequest).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 409,
      message: 'The submitted polygon overlaps 1 existing land lot.',
      problem: { conflictingLotIds: [FEATURE.id] },
    })
  })

  it.each([
    [
      400,
      { title: 'Invalid request', errors: [{ field: 'price', message: 'must be greater than 0' }] },
    ],
    [404, { title: 'Land lot not found', detail: 'No land lot with id x.' }],
    [422, { title: 'Invalid geometry', detail: 'Self-intersection at (0.5, 0.5)' }],
  ])('turns a %i problem into an ApiError with the problem body', async (status, body) => {
    server.use(http.get('*/api/land-lots/:id', () => problem(status, body)))

    const error = await getLandLot('x').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status, problem: { status, ...body } })
  })

  it('uses the title when the problem has no detail', async () => {
    server.use(http.get('*/api/land-lots/:id', () => problem(400, { title: 'Invalid request' })))

    await expect(getLandLot('x')).rejects.toThrow('Invalid request')
  })

  it('still reports the status when the error body is not a problem', async () => {
    server.use(
      http.get('*/api/land-lots/:id', () => new HttpResponse('Bad gateway', { status: 502 })),
    )

    const error = await getLandLot('x').catch((e: unknown) => e)

    expect(error).toMatchObject({
      status: 502,
      problem: null,
      message: 'Request failed with status 502',
    })
  })

  it('tolerates a problem content type with a broken body', async () => {
    server.use(
      http.get(
        '*/api/land-lots/:id',
        () =>
          new HttpResponse('{not json', {
            status: 500,
            headers: { 'Content-Type': 'application/problem+json' },
          }),
      ),
    )

    await expect(getLandLot('x')).rejects.toMatchObject({ status: 500, problem: null })
  })

  it('encodes the id in the path', async () => {
    let path = ''
    server.use(
      http.get('*/api/land-lots/:id', ({ request }) => {
        path = new URL(request.url).pathname
        return HttpResponse.json(FEATURE)
      }),
    )

    await getLandLot('a/b')

    expect(path).toBe('/api/land-lots/a%2Fb')
  })
})
