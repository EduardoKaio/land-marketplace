import { useEffect, useReducer, useState } from 'react'
import type { LandLotFilters } from '../../api/types'
import { BaseLayerSwitcher } from '../../map/BaseLayerSwitcher'
import { useLandLotLayer } from '../../map/useLandLotLayer'
import { AccountBar } from '../auth/AccountBar'
import { AuthDialog } from '../auth/AuthDialog'
import { useAuth } from '../auth/AuthContext'
import { FiltersPanel } from '../filters/FiltersPanel'
import { DrawLotTool } from '../land-lots/DrawLotTool'
import { LotPopup } from '../lot-popup/LotPopup'
import { initialMapModeState, mapModeReducer, type MapMode } from '../map-mode/mapMode'
import { Toolbar } from '../map-mode/Toolbar'
import { MyLotsPanel } from '../my-lots/MyLotsPanel'
import { SearchTool } from '../search/SearchTool'

interface AuthPrompt {
  reason?: string
  /** Tool to open once the user is logged in. */
  then?: MapMode
}

/**
 * Everything on top of the map. Each mode's tool is rendered only while that mode is active:
 * switching modes unmounts the previous tool, whose cleanup removes its interactions.
 */
export function MapWorkspace() {
  const [{ mode }, dispatch] = useReducer(mapModeReducer, initialMapModeState)
  // Shared by the map listing and the search, so one panel filters both
  const [filters, setFilters] = useState<LandLotFilters>({})
  const lotLayer = useLandLotLayer(filters)
  const { user, logout } = useAuth()
  const [authPrompt, setAuthPrompt] = useState<AuthPrompt | null>(null)
  const [myLotsOpen, setMyLotsOpen] = useState(false)

  // Covers both ways out: the Log out button and an expired session answered with 401
  useEffect(() => {
    if (!user) {
      dispatch({ type: 'logged-out' })
    }
  }, [user])

  const select = (selected: MapMode) => {
    if (selected === 'draw-lot' && !user) {
      setAuthPrompt({ reason: 'Log in to publish a lot.', then: 'draw-lot' })
      return
    }
    // Both panels use the right side of the map
    setMyLotsOpen(false)
    dispatch({ type: 'select', mode: selected })
  }

  const openMyLots = () => {
    if (mode !== 'explore') {
      dispatch({ type: 'select', mode: 'explore' })
    }
    setMyLotsOpen(true)
  }

  const afterLogin = () => {
    const next = authPrompt?.then
    setAuthPrompt(null)
    // Not through select(): in this closure user is still null, so it would ask to log in again
    if (next && next !== mode) {
      setMyLotsOpen(false)
      dispatch({ type: 'select', mode: next })
    }
  }

  return (
    <>
      <Toolbar mode={mode} onSelect={select} />
      <AccountBar
        onLogin={() => setAuthPrompt({})}
        onMyLots={openMyLots}
        onLogout={() => {
          setMyLotsOpen(false)
          logout()
        }}
      />
      <BaseLayerSwitcher />
      <FiltersPanel filters={filters} onChange={setFilters} />
      {/* No popup while drawing a lot: there a click adds a vertex */}
      {mode === 'explore' && <LotPopup layer={lotLayer} />}
      {mode === 'draw-lot' && user && <DrawLotTool lotSource={lotLayer.getSource()!} />}
      {mode === 'search' && (
        <SearchTool
          lotLayer={lotLayer}
          filters={filters}
          onClear={() => dispatch({ type: 'select', mode: 'explore' })}
          onClearFilters={() => setFilters({})}
        />
      )}
      {myLotsOpen && user && (
        <MyLotsPanel lotSource={lotLayer.getSource()!} onClose={() => setMyLotsOpen(false)} />
      )}
      {authPrompt && (
        <AuthDialog
          reason={authPrompt.reason}
          onClose={() => setAuthPrompt(null)}
          onAuthenticated={afterLogin}
        />
      )}
    </>
  )
}
