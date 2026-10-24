export type MapMode = 'explore' | 'draw-lot' | 'search'

export interface MapModeState {
  mode: MapMode
}

export type MapModeAction = { type: 'select'; mode: MapMode }

export const initialMapModeState: MapModeState = { mode: 'explore' }

/**
 * Single source of truth for the active tool. Selecting the active tool again turns it off and
 * goes back to exploring, like tool buttons in map editors.
 */
export function mapModeReducer(state: MapModeState, action: MapModeAction): MapModeState {
  switch (action.type) {
    case 'select':
      return { mode: state.mode === action.mode ? 'explore' : action.mode }
  }
}
