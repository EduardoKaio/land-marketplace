import { fromLonLat } from 'ol/proj'
import Polygon from 'ol/geom/Polygon'
import type { LandLotFeature, Position } from '../api/types'

/** The lot of the API contract example, near Ponta Negra (WGS84). */
export const PONTA_NEGRA_RING: Position[] = [
  [-35.196, -5.88],
  [-35.195, -5.88],
  [-35.195, -5.879],
  [-35.196, -5.879],
  [-35.196, -5.88],
]

/** The same square as drawn on the map (Web Mercator). */
export function pontaNegraPolygon(): Polygon {
  return new Polygon([PONTA_NEGRA_RING.map((position) => fromLonLat(position))])
}

export function landLotFeature(overrides: Partial<LandLotFeature> = {}): LandLotFeature {
  return {
    type: 'Feature',
    id: '3f0c2a8e-5b1d-4c7a-9e2f-1a6b8d4c0e93',
    geometry: { type: 'Polygon', coordinates: [PONTA_NEGRA_RING] },
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
    ...overrides,
  }
}
