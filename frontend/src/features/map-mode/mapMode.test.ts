import { describe, expect, it } from 'vitest'
import { initialMapModeState, mapModeReducer } from './mapMode'

describe('mapModeReducer', () => {
  it('starts exploring', () => {
    expect(initialMapModeState).toEqual({ mode: 'explore' })
  })

  it('switches to the selected tool', () => {
    expect(mapModeReducer({ mode: 'explore' }, { type: 'select', mode: 'draw-lot' })).toEqual({
      mode: 'draw-lot',
    })
    expect(mapModeReducer({ mode: 'draw-lot' }, { type: 'select', mode: 'search' })).toEqual({
      mode: 'search',
    })
  })

  it('turns the active tool off when it is selected again', () => {
    expect(mapModeReducer({ mode: 'draw-lot' }, { type: 'select', mode: 'draw-lot' })).toEqual({
      mode: 'explore',
    })
    expect(mapModeReducer({ mode: 'explore' }, { type: 'select', mode: 'explore' })).toEqual({
      mode: 'explore',
    })
  })
})
