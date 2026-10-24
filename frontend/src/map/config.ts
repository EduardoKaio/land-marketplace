/** Where the map opens. Center in WGS84 degrees, like every coordinate outside the map itself. */
export interface MapStartView {
  center: [lon: number, lat: number]
  zoom: number
}

/** Natal, Rio Grande do Norte. */
export const DEFAULT_START_VIEW: MapStartView = { center: [-35.2094, -5.7945], zoom: 12 }

interface StartViewEnv {
  VITE_MAP_CENTER?: string
  VITE_MAP_ZOOM?: string
}

/**
 * Reads the start view from VITE_MAP_CENTER ("lon,lat") and VITE_MAP_ZOOM, falling back to the
 * default for anything missing or invalid, so a typo in .env never leaves the map blank.
 */
export function readStartView(env: StartViewEnv = import.meta.env): MapStartView {
  return {
    center: parseCenter(env.VITE_MAP_CENTER) ?? DEFAULT_START_VIEW.center,
    zoom: parseZoom(env.VITE_MAP_ZOOM) ?? DEFAULT_START_VIEW.zoom,
  }
}

function parseCenter(text: string | undefined): [number, number] | null {
  const parts = text?.split(',').map((part) => Number(part.trim()))
  if (parts?.length !== 2) {
    return null
  }
  const [lon, lat] = parts
  const valid = Math.abs(lon) <= 180 && Math.abs(lat) <= 90
  return valid ? [lon, lat] : null
}

function parseZoom(text: string | undefined): number | null {
  const zoom = Number(text)
  return text && zoom >= 0 && zoom <= 22 ? zoom : null
}
