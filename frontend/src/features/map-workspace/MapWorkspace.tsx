import { useReducer } from 'react'
import { BaseLayerSwitcher } from '../../map/BaseLayerSwitcher'
import { useLandLotLayer } from '../../map/useLandLotLayer'
import { DrawLotTool } from '../land-lots/DrawLotTool'
import { initialMapModeState, mapModeReducer } from '../map-mode/mapMode'
import { Toolbar } from '../map-mode/Toolbar'

/**
 * Everything on top of the map. Each mode's tool is rendered only while that mode is active:
 * switching modes unmounts the previous tool, whose cleanup removes its interactions.
 */
export function MapWorkspace() {
  const [{ mode }, dispatch] = useReducer(mapModeReducer, initialMapModeState)
  const lotLayer = useLandLotLayer()

  return (
    <>
      <Toolbar mode={mode} onSelect={(selected) => dispatch({ type: 'select', mode: selected })} />
      <BaseLayerSwitcher />
      {mode === 'draw-lot' && <DrawLotTool lotSource={lotLayer.getSource()!} />}
    </>
  )
}
