import type Feature from 'ol/Feature'
import GeoJSON from 'ol/format/GeoJSON'
import type Polygon from 'ol/geom/Polygon'
import VectorLayer from 'ol/layer/Vector'
import { bbox as bboxStrategy } from 'ol/loadingstrategy'
import { transformExtent, type ProjectionLike } from 'ol/proj'
import VectorSource from 'ol/source/Vector'
import type { Extent } from 'ol/extent'
import { listLandLotsInBbox } from '../api/landLots'
import type { BoundingBox, LandLotFeature, LandLotFeatureCollection } from '../api/types'
import { CONFLICT_PROPERTY, landLotStyle } from './landLotStyle'

export type LandLotSource = VectorSource<Feature<Polygon>>
export type LandLotLayer = VectorLayer<LandLotSource>

// The API speaks WGS84; the map view is Web Mercator. This is the only place lots are converted
const geoJson = new GeoJSON({ dataProjection: 'EPSG:4326', featureProjection: 'EPSG:3857' })

/**
 * The map extent as an API bounding box. The extent can reach past the antimeridian when the
 * world repeats horizontally, and the API only accepts [-180, 180], so it is clamped. Returns
 * null when nothing of the box is left.
 */
export function toApiBbox(
  extent: Extent,
  projection: ProjectionLike = 'EPSG:3857',
): BoundingBox | null {
  const [minLon, minLat, maxLon, maxLat] = transformExtent(extent, projection, 'EPSG:4326')
  const box: BoundingBox = [
    clamp(minLon, 180),
    clamp(minLat, 90),
    clamp(maxLon, 180),
    clamp(maxLat, 90),
  ]
  return box[0] < box[2] && box[1] < box[3] ? box : null
}

function clamp(value: number, limit: number): number {
  return Math.min(Math.max(value, -limit), limit)
}

export function readLandLotFeatures(
  data: LandLotFeatureCollection | LandLotFeature,
): Feature<Polygon>[] {
  return geoJson.readFeatures(data) as Feature<Polygon>[]
}

/** Extent of a lot in the map projection, to zoom the view onto it. */
export function lotExtent(lot: LandLotFeature): Extent {
  return readLandLotFeatures(lot)[0].getGeometry()!.getExtent()
}

/**
 * Lots loaded for the visible area: the bbox strategy calls the loader with the extent of each
 * new view, and the source skips extents it already loaded.
 */
export function createLandLotSource(): LandLotSource {
  const source: LandLotSource = new VectorSource({
    strategy: bboxStrategy,
    loader: (extent, _resolution, projection, success, failure) => {
      const bbox = toApiBbox(extent, projection)
      if (!bbox) {
        success?.([])
        return
      }
      listLandLotsInBbox(bbox)
        .then((collection) => {
          // Lots with an id already in the source are skipped, so overlapping boxes add no duplicates
          const features = readLandLotFeatures(collection)
          source.addFeatures(features)
          if (collection.truncated) {
            // The server capped the answer: forget this extent so zooming in loads the rest
            source.removeLoadedExtent(extent)
          }
          success?.(features)
        })
        .catch(() => {
          // Lets the next view of this area try again
          source.removeLoadedExtent(extent)
          failure?.()
        })
    },
  })
  return source
}

export function createLandLotLayer(): LandLotLayer {
  return new VectorLayer({ source: createLandLotSource(), style: landLotStyle })
}

/** Marks the given lots as conflicting (drawn in red), clearing any previous marks. */
export function highlightConflicts(source: LandLotSource, ids: string[]): void {
  clearConflicts(source)
  for (const id of ids) {
    source.getFeatureById(id)?.set(CONFLICT_PROPERTY, true)
  }
}

export function clearConflicts(source: LandLotSource): void {
  for (const feature of source.getFeatures()) {
    if (feature.get(CONFLICT_PROPERTY)) {
      feature.unset(CONFLICT_PROPERTY)
    }
  }
}
