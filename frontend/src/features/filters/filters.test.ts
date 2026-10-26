import { describe, expect, it } from 'vitest'
import {
  countActiveFilters,
  filterFormSchema,
  toFilters,
  toFormValues,
  type FilterFormValues,
} from './filters'

const EMPTY: FilterFormValues = {
  minPrice: '',
  maxPrice: '',
  minArea: '',
  maxArea: '',
  maxPricePerSquareMeter: '',
  status: '',
}

function parse(values: Partial<FilterFormValues>) {
  return filterFormSchema.safeParse({ ...EMPTY, ...values })
}

function errorsOf(values: Partial<FilterFormValues>): string[] {
  return (
    parse(values).error?.issues.map((issue) => `${String(issue.path[0])}: ${issue.message}`) ?? []
  )
}

describe('filter form', () => {
  it('turns empty fields into no filter', () => {
    const result = parse({})

    expect(result.success).toBe(true)
    expect(toFilters(result.data!)).toEqual({})
  })

  it('parses numbers and the status', () => {
    const result = parse({ minPrice: ' 1000 ', maxArea: '2500.5', status: 'SOLD' })

    expect(toFilters(result.data!)).toEqual({ minPrice: 1000, maxArea: 2500.5, status: 'SOLD' })
  })

  it('rejects text and negative values', () => {
    expect(errorsOf({ minPrice: 'abc', maxPricePerSquareMeter: '-1' })).toEqual([
      'minPrice: Enter a number',
      'maxPricePerSquareMeter: Use zero or more',
    ])
  })

  it('rejects a maximum below the minimum, like the server', () => {
    expect(errorsOf({ minPrice: '500', maxPrice: '100', minArea: '9', maxArea: '1' })).toEqual([
      'maxPrice: Must not be below the minimum price',
      'maxArea: Must not be below the minimum area',
    ])
  })

  it('accepts equal bounds', () => {
    expect(errorsOf({ minPrice: '100', maxPrice: '100' })).toEqual([])
  })

  it('shows the applied filters back as text', () => {
    expect(toFormValues({ minPrice: 0, maxArea: 300, status: 'AVAILABLE' })).toEqual({
      ...EMPTY,
      minPrice: '0',
      maxArea: '300',
      status: 'AVAILABLE',
    })
  })
})

describe('countActiveFilters', () => {
  it('counts the filters that are set, zero included', () => {
    expect(countActiveFilters({})).toBe(0)
    expect(countActiveFilters({ minPrice: 0, status: 'SOLD', maxArea: undefined })).toBe(2)
  })
})
