import { describe, expect, it } from 'vitest'
import { isBrazilianPhone, maskPhone, telHref } from './phone'

describe('maskPhone', () => {
  it.each([
    ['', ''],
    ['8', '(8'],
    ['84', '(84'],
    ['849', '(84) 9'],
    ['849999', '(84) 9999'],
    ['8499998', '(84) 9999-8'],
    ['8433334444', '(84) 3333-4444'],
    ['84999998888', '(84) 99999-8888'],
  ])('formats %j as %j while typing', (typed, masked) => {
    expect(maskPhone(typed)).toBe(masked)
  })

  it('drops letters and symbols', () => {
    expect(maskPhone('84a 9999-x8888')).toBe('(84) 9999-8888')
  })

  it('keeps at most 11 digits', () => {
    expect(maskPhone('849999988887')).toBe('(84) 99999-8888')
  })

  it('removes the country code of a pasted international number', () => {
    expect(maskPhone('+55 84 99999-8888')).toBe('(84) 99999-8888')
    expect(maskPhone('+55 (84) 3333-4444')).toBe('(84) 3333-4444')
  })

  // 55 is also a DDD: without the plus sign it stays the area code
  it('keeps 55 as the DDD when there is no plus sign', () => {
    expect(maskPhone('55999998888')).toBe('(55) 99999-8888')
    expect(maskPhone('(55) 99999-88881')).toBe('(55) 99999-8888')
  })

  it('masks a phone typed one key at a time', () => {
    const typed = '84999998888'.split('').reduce((value, key) => maskPhone(value + key), '')

    expect(typed).toBe('(84) 99999-8888')
  })

  it('re-masks a value whose punctuation was deleted', () => {
    expect(maskPhone('(84) 99998888')).toBe('(84) 9999-8888')
  })
})

describe('isBrazilianPhone', () => {
  it.each(['(84) 3333-4444', '(84) 99999-8888', '(11) 2345-6789'])('accepts %s', (phone) => {
    expect(isBrazilianPhone(phone)).toBe(true)
  })

  it.each([
    '(84) 9999-8',
    '84999998888',
    '(04) 99999-8888',
    '(80) 3333-4444',
    '(84) 6333-4444',
    '(84) 89999-8888',
  ])('rejects %s', (phone) => {
    expect(isBrazilianPhone(phone)).toBe(false)
  })
})

describe('telHref', () => {
  it('adds the Brazilian country code to a masked phone', () => {
    expect(telHref('(84) 99999-8888')).toBe('tel:+5584999998888')
  })

  it('keeps the country code of an international phone', () => {
    expect(telHref('+55 84 90000-0001')).toBe('tel:+5584900000001')
  })
})
