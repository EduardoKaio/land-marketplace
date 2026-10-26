import { z } from 'zod'
import type { LandLotFilters, LandLotStatus } from '../../api/types'

/** How many filters are set, for the "Filters (2)" button. */
export function countActiveFilters(filters: LandLotFilters): number {
  return Object.values(filters).filter((value) => value !== undefined).length
}

/** Form values are the input texts; an empty text means "no filter". */
export interface FilterFormValues {
  minPrice: string
  maxPrice: string
  minArea: string
  maxArea: string
  maxPricePerSquareMeter: string
  status: '' | LandLotStatus
}

const amount = z
  .string()
  .trim()
  .transform((text) => (text === '' ? undefined : Number(text)))
  .pipe(z.number({ error: 'Enter a number' }).min(0, 'Use zero or more').optional())

/** Mirrors LandLotFilterRequest of the backend: non-negative bounds, minimum not above maximum. */
export const filterFormSchema = z
  .object({
    minPrice: amount,
    maxPrice: amount,
    minArea: amount,
    maxArea: amount,
    maxPricePerSquareMeter: amount,
    status: z.enum(['', 'AVAILABLE', 'SOLD']).transform((status) => status || undefined),
  })
  .refine((f) => !isInverted(f.minPrice, f.maxPrice), {
    error: 'Must not be below the minimum price',
    path: ['maxPrice'],
  })
  .refine((f) => !isInverted(f.minArea, f.maxArea), {
    error: 'Must not be below the minimum area',
    path: ['maxArea'],
  })

function isInverted(min: number | undefined, max: number | undefined): boolean {
  return min !== undefined && max !== undefined && min > max
}

/** The parsed form, without the unset fields, as the API client takes it. */
export function toFilters(values: z.output<typeof filterFormSchema>): LandLotFilters {
  return Object.fromEntries(
    Object.entries(values).filter(([, value]) => value !== undefined),
  ) as LandLotFilters
}

export function toFormValues(filters: LandLotFilters): FilterFormValues {
  const text = (value: number | undefined) => (value === undefined ? '' : String(value))
  return {
    minPrice: text(filters.minPrice),
    maxPrice: text(filters.maxPrice),
    minArea: text(filters.minArea),
    maxArea: text(filters.maxArea),
    maxPricePerSquareMeter: text(filters.maxPricePerSquareMeter),
    status: filters.status ?? '',
  }
}
