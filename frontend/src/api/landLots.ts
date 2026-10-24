import { requestJson } from './http'
import type {
  BoundingBox,
  CircleSearch,
  CreateLandLotRequest,
  LandLotFeature,
  LandLotFeatureCollection,
} from './types'

const LAND_LOTS = '/api/land-lots'

/** Registers a lot. Rejects with ApiError 400, 409 (overlap) or 422 (invalid geometry). */
export function createLandLot(request: CreateLandLotRequest): Promise<LandLotFeature> {
  return requestJson(LAND_LOTS, { method: 'POST', body: request })
}

/** Lots in the visible map area, largest first, capped by the server (see `truncated`). */
export function listLandLotsInBbox(
  bbox: BoundingBox,
  signal?: AbortSignal,
): Promise<LandLotFeatureCollection> {
  return requestJson(LAND_LOTS, { query: { bbox: bbox.join(',') }, signal })
}

/** Lots that intersect the circle. */
export function searchLandLots(
  { lon, lat, radius }: CircleSearch,
  signal?: AbortSignal,
): Promise<LandLotFeatureCollection> {
  return requestJson(`${LAND_LOTS}/search`, { query: { lon, lat, radius }, signal })
}

export function getLandLot(id: string, signal?: AbortSignal): Promise<LandLotFeature> {
  return requestJson(`${LAND_LOTS}/${encodeURIComponent(id)}`, { signal })
}
