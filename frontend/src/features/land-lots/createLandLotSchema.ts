import { z } from 'zod'
import type { CreateLandLotRequest, PolygonGeometry } from '../../api/types'

const EMAIL = z.email()

function hasAtMostTwoDecimals(value: number): boolean {
  const cents = value * 100
  return Math.abs(cents - Math.round(cents)) < 1e-6
}

/** Mirrors the backend Bean Validation rules, so most mistakes are caught before a request. */
export const createLandLotSchema = z.object({
  title: z.string().trim().min(1, 'Enter a title').max(120, 'Use at most 120 characters'),
  price: z
    .number({ error: 'Enter a price' })
    .positive('The price must be greater than zero')
    .refine(hasAtMostTwoDecimals, 'Use at most 2 decimal places'),
  description: z.string().trim().min(1, 'Enter a description'),
  contact: z
    .object({
      name: z.string().trim().min(1, 'Enter a contact name').max(120, 'Use at most 120 characters'),
      phone: z.string().trim().max(30, 'Use at most 30 characters'),
      email: z
        .string()
        .trim()
        .refine((email) => email === '' || EMAIL.safeParse(email).success, 'Enter a valid email'),
    })
    .refine((contact) => contact.phone !== '' || contact.email !== '', {
      error: 'Enter a phone or an email',
      path: ['phone'],
    }),
})

export type CreateLandLotFormValues = z.infer<typeof createLandLotSchema>

export const EMPTY_FORM: Partial<CreateLandLotFormValues> = {
  title: '',
  description: '',
  contact: { name: '', phone: '', email: '' },
}

/** Form values plus the drawn geometry as the API body; empty contact fields are left out. */
export function toCreateLandLotRequest(
  values: CreateLandLotFormValues,
  geometry: PolygonGeometry,
): CreateLandLotRequest {
  const { name, phone, email } = values.contact
  return {
    title: values.title,
    price: values.price,
    description: values.description,
    contact: { name, ...(phone && { phone }), ...(email && { email }) },
    geometry,
  }
}
