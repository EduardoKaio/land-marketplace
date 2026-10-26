import { requestJson } from './http'
import type {
  BoundingBox,
  CircleSearch,
  CreateLandLotRequest,
  LandLotFeature,
  LandLotFeatureCollection,
  LandLotFilters,
} from './types'

const LAND_LOTS = '/api/land-lots'

/** Registers a lot. Rejects with ApiError 400, 409 (overlap) or 422 (invalid geometry). */
export function createLandLot(request: CreateLandLotRequest): Promise<LandLotFeature> {
  return requestJson(LAND_LOTS, { method: 'POST', body: request })
}

/** The filters as query parameters, leaving out the ones that are not set. */
export function filterQuery(filters: LandLotFilters): Record<string, string | number> {
  const query: Record<string, string | number> = {}
  for (const [name, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '') {
      query[name] = value
    }
  }
  return query
}

/** Lots in the visible map area, largest first, capped by the server (see `truncated`). */
export function listLandLotsInBbox(
  bbox: BoundingBox,
  filters: LandLotFilters = {},
  signal?: AbortSignal,
): Promise<LandLotFeatureCollection> {
  return requestJson(LAND_LOTS, {
    query: { bbox: bbox.join(','), ...filterQuery(filters) },
    signal,
  })
}

/** Lots that intersect the circle. */
export function searchLandLots(
  { lon, lat, radius }: CircleSearch,
  filters: LandLotFilters = {},
  signal?: AbortSignal,
): Promise<LandLotFeatureCollection> {
  return requestJson(`${LAND_LOTS}/search`, {
    query: { lon, lat, radius, ...filterQuery(filters) },
    signal,
  })
}

/** Lots of the logged in account, newest first. Needs a token. */
export function listMyLandLots(signal?: AbortSignal): Promise<LandLotFeatureCollection> {
  return requestJson('/api/me/land-lots', { signal })
}

/** Only the owner can delete; others get ApiError 403. */
export function deleteLandLot(id: string): Promise<void> {
  return requestJson(`${LAND_LOTS}/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

export function getLandLot(id: string, signal?: AbortSignal): Promise<LandLotFeature> {
  return requestJson(`${LAND_LOTS}/${encodeURIComponent(id)}`, { signal })
}
