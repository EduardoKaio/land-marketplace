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
        contact: { name: 'x'.repeat(121), phone: '84999998888', email: 'not an email' },
      }),
    ).toEqual({
      title: 'Enter a title',
      price: 'Enter a price',
      description: 'Enter a description',
      'contact.name': 'Use at most 120 characters',
      'contact.phone': 'Enter a phone like (84) 99999-8888',
      'contact.email': 'Enter a valid email',
    })
  })

  it.each(['(84) 3333-4444', '(84) 99999-8888'])('accepts the phone %s', (phone) => {
    expect(issues({ ...VALID, contact: { ...VALID.contact, phone } })).toEqual({})
  })

  // A mask half filled in, or a number the backend would reject
  it.each(['(84) 9999', '(84) 6333-4444', '(04) 99999-8888'])('rejects the phone %s', (phone) => {
    expect(issues({ ...VALID, contact: { ...VALID.contact, phone } })).toEqual({
      'contact.phone': 'Enter a phone like (84) 99999-8888',
    })
  })

  // The backend's @Email alone would take these; both sides now require a full domain
  it.each(['ana@localhost', 'ana@example.c'])('rejects the email %s', (email) => {
    expect(issues({ ...VALID, contact: { ...VALID.contact, email } })).toEqual({
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
      contact: { name: 'Kaio', phone: '(84) 99999-0000', email: 'k@example.com' },
    }

    expect(toCreateLandLotRequest(values, geometry).contact).toEqual(values.contact)
  })
})
