/**
 * Types of the backend API contract. Every coordinate is WGS84 longitude and latitude in degrees
 * (EPSG:4326); converting to and from the map projection happens at the map boundary, not here.
 */

/** [longitude, latitude], the GeoJSON position order. */
export type Position = [number, number]

/** GeoJSON Polygon: an exterior ring followed by optional holes, each ring closed. */
export interface PolygonGeometry {
  type: 'Polygon'
  coordinates: Position[][]
}

export type LandLotStatus = 'AVAILABLE' | 'SOLD'

/** At least one of phone and email is present. */
export interface Contact {
  name: string
  phone?: string | null
  email?: string | null
}

export interface LandLotProperties {
  title: string
  price: number
  areaSquareMeters: number
  pricePerSquareMeter: number
  description: string
  contact: Contact
  status: LandLotStatus
  /** ISO 8601 instant in UTC. */
  createdAt: string
}

export interface LandLotFeature {
  type: 'Feature'
  id: string
  geometry: PolygonGeometry
  properties: LandLotProperties
}

export interface LandLotFeatureCollection {
  type: 'FeatureCollection'
  features: LandLotFeature[]
  /** True when more lots matched than the server returns for one request. */
  truncated: boolean
}

export interface CreateLandLotRequest {
  title: string
  price: number
  description: string
  contact: Contact
  geometry: PolygonGeometry
}

/** [minLon, minLat, maxLon, maxLat] in degrees. */
export type BoundingBox = [number, number, number, number]

export interface CircleSearch {
  lon: number
  lat: number
  /** Geodesic radius in meters. */
  radius: number
}

export interface User {
  id: string
  name: string
  email: string
}

/** Answer of register and login. */
export interface AuthResponse {
  accessToken: string
  tokenType: 'Bearer'
  /** Seconds until the token expires. */
  expiresIn: number
  user: User
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface FieldError {
  field: string
  message: string
}

/** RFC 9457 error body, with the extension members this API uses. */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  instance?: string
  /** Invalid fields of a 400 validation error. */
  errors?: FieldError[]
  /** Lots that a rejected polygon overlaps (409). */
  conflictingLotIds?: string[]
  /** Largest radius accepted by the circle search. */
  maxRadiusMeters?: number
}
