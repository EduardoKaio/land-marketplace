import { describe, expect, it } from 'vitest'
import { DEFAULT_START_VIEW, readStartView } from './config'

describe('readStartView', () => {
  it('opens on Natal when nothing is configured', () => {
    expect(readStartView({})).toEqual(DEFAULT_START_VIEW)
  })

  it('reads center and zoom from the environment', () => {
    expect(readStartView({ VITE_MAP_CENTER: ' -46.63, -23.55 ', VITE_MAP_ZOOM: '10' })).toEqual({
      center: [-46.63, -23.55],
      zoom: 10,
    })
  })

  it.each(['abc', '-35.2', '-35.2,-5.8,1', '-181,0', '0,91', ''])(
    'falls back to the default center for %j',
    (center) => {
      expect(readStartView({ VITE_MAP_CENTER: center }).center).toEqual(DEFAULT_START_VIEW.center)
    },
  )

  it.each(['abc', '-1', '23', ''])('falls back to the default zoom for %j', (zoom) => {
    expect(readStartView({ VITE_MAP_ZOOM: zoom }).zoom).toBe(DEFAULT_START_VIEW.zoom)
  })
})
