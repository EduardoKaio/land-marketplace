import { z } from 'zod'
import { ApiError } from '../../api/http'

const email = z.string().trim().pipe(z.email('Enter a valid email'))

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'Enter your password'),
})

/** Mirrors RegisterRequest of the backend, so a weak password is caught before a request. */
export const registerSchema = z.object({
  name: z.string().trim().min(1, 'Enter your name').max(120, 'Use at most 120 characters'),
  email: email.pipe(z.string().max(254, 'Use at most 254 characters')),
  password: z
    .string()
    .min(8, 'Use at least 8 characters')
    .max(64, 'Use at most 64 characters')
    .regex(/\p{L}/u, 'Include at least one letter')
    .regex(/\d/, 'Include at least one digit'),
})

export type LoginValues = z.infer<typeof loginSchema>
export type RegisterValues = z.infer<typeof registerSchema>

export type AuthField = 'name' | 'email' | 'password'

export interface AuthFailure {
  fieldErrors: { field: AuthField; message: string }[]
  /** Shown above the form when the error belongs to no field. */
  message: string | null
}

const FIELDS: readonly string[] = ['name', 'email', 'password']

/** How a failed login or registration is shown: under a field when the API names one. */
export function describeAuthFailure(error: unknown): AuthFailure {
  if (!(error instanceof ApiError)) {
    return { fieldErrors: [], message: 'Could not reach the server. Try again in a moment.' }
  }
  if (error.status === 409) {
    return { fieldErrors: [{ field: 'email', message: error.message }], message: null }
  }
  const fieldErrors = (error.problem?.errors ?? [])
    .filter(({ field }) => FIELDS.includes(field))
    .map(({ field, message }) => ({ field: field as AuthField, message }))
  if (fieldErrors.length > 0) {
    return { fieldErrors, message: null }
  }
  // 401 carries "Invalid email or password." from the server
  return { fieldErrors: [], message: error.message }
}
