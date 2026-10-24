import Feature from 'ol/Feature'
import { describe, expect, it } from 'vitest'
import { CONFLICT_PROPERTY, landLotStyle } from './landLotStyle'

function strokeColor(feature: Feature): unknown {
  return landLotStyle(feature).getStroke()?.getColor()
}

describe('landLotStyle', () => {
  it('draws available lots in green and sold lots in gray', () => {
    expect(strokeColor(new Feature({ status: 'AVAILABLE' }))).toBe('#2e7d32')
    expect(strokeColor(new Feature({ status: 'SOLD' }))).toBe('#6b7280')
  })

  it('draws conflicting lots in red whatever their status', () => {
    expect(strokeColor(new Feature({ status: 'SOLD', [CONFLICT_PROPERTY]: true }))).toBe('#d32f2f')
  })

  it('reuses the same style objects', () => {
    expect(landLotStyle(new Feature({ status: 'SOLD' }))).toBe(
      landLotStyle(new Feature({ status: 'SOLD' })),
    )
  })
})
