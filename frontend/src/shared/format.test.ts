import { describe, expect, it } from 'vitest'
import { formatArea, formatCurrency, formatDistance, pricePerSquareMeter } from './format'

describe('formatArea', () => {
  it.each([
    [0, '0 m²'],
    [8450.4, '8,450 m²'],
    [9999.4, '9,999 m²'],
    [10_000, '1.00 ha'],
    [12_246.04, '1.22 ha'],
    [2_500_000, '250.00 ha'],
  ])('formats %d m² as %s', (squareMeters, expected) => {
    expect(formatArea(squareMeters)).toBe(expected)
  })
})

describe('formatDistance', () => {
  it.each([
    [0, '0 m'],
    [850.4, '850 m'],
    [999.4, '999 m'],
    [999.6, '1.00 km'],
    [1250, '1.25 km'],
    [50_000, '50.00 km'],
  ])('formats %d m as %s', (meters, expected) => {
    expect(formatDistance(meters)).toBe(expected)
  })
})

describe('pricePerSquareMeter', () => {
  it('divides the price by the area', () => {
    expect(pricePerSquareMeter(250_000, 12_246.04)).toBeCloseTo(20.41, 2)
  })

  it.each([
    [undefined, 100],
    [Number.NaN, 100],
    [0, 100],
    [-5, 100],
    [1000, null],
    [1000, 0],
  ])('is unknown for price %s and area %s', (price, area) => {
    expect(pricePerSquareMeter(price, area)).toBeNull()
  })
})

describe('formatCurrency', () => {
  it('formats Brazilian reais', () => {
    expect(formatCurrency(20.4123)).toBe('R$20.41')
  })
})
