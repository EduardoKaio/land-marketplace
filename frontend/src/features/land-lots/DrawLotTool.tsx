import { useEffect } from 'react'
import type { LandLotFeature } from '../../api/types'
import {
  clearConflicts,
  highlightConflicts,
  readLandLotFeatures,
  type LandLotSource,
} from '../../map/landLotLayer'
import { CreateLandLotPanel } from './CreateLandLotPanel'
import { useDrawLot } from './useDrawLot'
import './landLots.css'

/** Draw-lot mode: rendered only while the mode is active, so unmounting removes its interactions. */
export function DrawLotTool({ lotSource }: { lotSource: LandLotSource }) {
  const { draft, areaM2, reset } = useDrawLot(lotSource)

  // Leaving the mode also clears the red marks of a rejected attempt
  useEffect(() => () => clearConflicts(lotSource), [lotSource])

  const handleCreated = (lot: LandLotFeature) => {
    lotSource.addFeatures(readLandLotFeatures(lot))
    clearConflicts(lotSource)
    reset()
  }

  const handleCancel = () => {
    clearConflicts(lotSource)
    reset()
  }

  if (!draft || areaM2 === null) {
    return (
      <p className="map-hint" role="status">
        Click to add vertices, double-click to finish. Esc cancels, Backspace removes the last
        vertex.
      </p>
    )
  }
  return (
    <CreateLandLotPanel
      draft={draft}
      areaM2={areaM2}
      onCreated={handleCreated}
      onConflict={(ids) => highlightConflicts(lotSource, ids)}
      onCancel={handleCancel}
    />
  )
}
