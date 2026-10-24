import { ApiError } from '../../api/http'

export type FormField =
  'title' | 'price' | 'description' | 'contact.name' | 'contact.phone' | 'contact.email'

/** API field names (from the 400 "errors" list) to form fields. */
const FIELDS: Record<string, FormField> = {
  title: 'title',
  price: 'price',
  description: 'description',
  'contact.name': 'contact.name',
  'contact.phone': 'contact.phone',
  'contact.email': 'contact.email',
  // Cross-field rule of the backend; shown under the phone, like the form's own rule
  'contact.phoneOrEmailPresent': 'contact.phone',
}

export interface SubmitFailure {
  /** Lots the polygon overlaps (409), to highlight on the map. */
  conflictingLotIds: string[]
  /** Errors to show under the form fields. */
  fieldErrors: { field: FormField; message: string }[]
  /** Text for a toast, when something cannot be shown on a field. */
  message: string | null
}

const GENERIC_MESSAGE = 'Could not publish the lot. Try again in a moment.'

/** Decides how a failed creation is shown to the user. */
export function describeSubmitFailure(error: unknown): SubmitFailure {
  const failure: SubmitFailure = { conflictingLotIds: [], fieldErrors: [], message: null }
  if (!(error instanceof ApiError)) {
    return { ...failure, message: GENERIC_MESSAGE }
  }
  const problem = error.problem
  switch (error.status) {
    case 409: {
      const ids = problem?.conflictingLotIds ?? []
      return { ...failure, conflictingLotIds: ids, message: overlapMessage(ids.length) }
    }
    case 400: {
      const unmatched: string[] = []
      for (const { field, message } of problem?.errors ?? []) {
        const formField = FIELDS[field]
        if (formField) {
          failure.fieldErrors.push({ field: formField, message })
        } else {
          unmatched.push(`${field} ${message}`)
        }
      }
      const fallback =
        failure.fieldErrors.length === 0 ? (problem?.detail ?? GENERIC_MESSAGE) : null
      return { ...failure, message: unmatched.length > 0 ? unmatched.join('; ') : fallback }
    }
    case 422:
      return {
        ...failure,
        message: `The polygon is not valid: ${problem?.detail ?? 'unknown reason'}.`,
      }
    default:
      return { ...failure, message: GENERIC_MESSAGE }
  }
}

function overlapMessage(count: number): string {
  const lots = count === 1 ? '1 existing lot' : `${count} existing lots`
  return `This area overlaps ${lots}. Adjust the polygon; sharing a border is allowed.`
}
