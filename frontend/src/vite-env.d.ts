/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Start center of the map as "lon,lat" in degrees. */
  readonly VITE_MAP_CENTER?: string
  /** Start zoom level of the map. */
  readonly VITE_MAP_ZOOM?: string
}
