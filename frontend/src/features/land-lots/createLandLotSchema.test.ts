import { describe, expect, it } from 'vitest'
import type { PolygonGeometry } from '../../api/types'
import {
  createLandLotSchema,
  toCreateLandLotRequest,
  type CreateLandLotFormValues,
} from './createLandLotSchema'

const VALID: CreateLandLotFormValues = {
  title: 'Corner lot',
  price: 250000.5,
  description: 'Flat terrain',
  contact: { name: 'Kaio', phone: '', email: 'kaio@example.com' },
}

function issues(values: unknown): Record<string, string> {
  const result = createLandLotSchema.safeParse(values)
  return result.success
    ? {}
    : Object.fromEntries(result.error.issues.map((issue) => [issue.path.join('.'), issue.message]))
}

describe('createLandLotSchema', () => {
  it('accepts a complete lot', () => {
    expect(issues(VALID)).toEqual({})
  })

  it('rejects what the backend would reject', () => {
    expect(
      issues({
        ...VALID,
        title: '  ',
        price: Number.NaN,
        description: '',
        contact: { name: 'x'.repeat(121), phone: '1'.repeat(31), email: 'not an email' },
      }),
    ).toEqual({
      title: 'Enter a title',
      price: 'Enter a price',
      description: 'Enter a description',
      'contact.name': 'Use at most 120 characters',
      'contact.phone': 'Use at most 30 characters',
      'contact.email': 'Enter a valid email',
    })
  })

  it.each([
    [0, 'The price must be greater than zero'],
    [-1, 'The price must be greater than zero'],
    [10.001, 'Use at most 2 decimal places'],
  ])('rejects the price %d', (price, message) => {
    expect(issues({ ...VALID, price })).toEqual({ price: message })
  })

  it('requires a phone or an email', () => {
    expect(issues({ ...VALID, contact: { name: 'Kaio', phone: ' ', email: '' } })).toEqual({
      'contact.phone': 'Enter a phone or an email',
    })
  })
})

describe('toCreateLandLotRequest', () => {
  const geometry: PolygonGeometry = { type: 'Polygon', coordinates: [] }

  it('leaves empty contact fields out of the request', () => {
    expect(toCreateLandLotRequest(VALID, geometry)).toEqual({
      title: 'Corner lot',
      price: 250000.5,
      description: 'Flat terrain',
      contact: { name: 'Kaio', email: 'kaio@example.com' },
      geometry,
    })
  })

  it('keeps both channels when both are given', () => {
    const values = {
      ...VALID,
      contact: { name: 'Kaio', phone: '+55 84 99999-0000', email: 'k@example.com' },
    }

    expect(toCreateLandLotRequest(values, geometry).contact).toEqual(values.contact)
  })
})
