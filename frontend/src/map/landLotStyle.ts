import type { FeatureLike } from 'ol/Feature'
import Fill from 'ol/style/Fill'
import Stroke from 'ol/style/Stroke'
import Style from 'ol/style/Style'

function polygonStyle(color: string, fillAlpha: string, width: number): Style {
  return new Style({
    fill: new Fill({ color: `${color}${fillAlpha}` }),
    stroke: new Stroke({ color, width }),
  })
}

// Built once: a style function runs for every feature on every frame
const AVAILABLE = polygonStyle('#2e7d32', '40', 2)
const SOLD = polygonStyle('#6b7280', '40', 2)
const CONFLICT = polygonStyle('#d32f2f', '66', 3)

/** Property set on lots that a rejected polygon overlaps. */
export const CONFLICT_PROPERTY = 'conflict'

export function landLotStyle(feature: FeatureLike): Style {
  if (feature.get(CONFLICT_PROPERTY)) {
    return CONFLICT
  }
  return feature.get('status') === 'SOLD' ? SOLD : AVAILABLE
}
