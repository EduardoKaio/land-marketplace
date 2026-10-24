import { describe, expect, it } from 'vitest'
import { ApiError } from '../../api/http'
import { describeSubmitFailure } from './serverErrors'

describe('describeSubmitFailure', () => {
  it('explains an overlap and lists the lots to highlight', () => {
    const failure = describeSubmitFailure(
      new ApiError(409, { status: 409, conflictingLotIds: ['a', 'b'] }),
    )

    expect(failure).toEqual({
      conflictingLotIds: ['a', 'b'],
      fieldErrors: [],
      message:
        'This area overlaps 2 existing lots. Adjust the polygon; sharing a border is allowed.',
    })
  })

  it('uses the singular for one lot and copes with a 409 without ids', () => {
    expect(
      describeSubmitFailure(new ApiError(409, { status: 409, conflictingLotIds: ['a'] })).message,
    ).toContain('overlaps 1 existing lot.')
    expect(describeSubmitFailure(new ApiError(409, null)).conflictingLotIds).toEqual([])
  })

  it('puts validation errors on the form fields', () => {
    const failure = describeSubmitFailure(
      new ApiError(400, {
        status: 400,
        errors: [
          { field: 'price', message: 'must be greater than 0' },
          { field: 'contact.phoneOrEmailPresent', message: 'phone or email is required' },
        ],
      }),
    )

    expect(failure.fieldErrors).toEqual([
      { field: 'price', message: 'must be greater than 0' },
      { field: 'contact.phone', message: 'phone or email is required' },
    ])
    expect(failure.message).toBeNull()
  })

  it('reports errors of fields the form does not have in a toast', () => {
    const failure = describeSubmitFailure(
      new ApiError(400, {
        status: 400,
        errors: [
          { field: 'title', message: 'must not be blank' },
          { field: 'geometry', message: 'must not be null' },
        ],
      }),
    )

    expect(failure.fieldErrors).toEqual([{ field: 'title', message: 'must not be blank' }])
    expect(failure.message).toBe('geometry must not be null')
  })

  it('shows the detail of a 400 without field errors', () => {
    expect(
      describeSubmitFailure(new ApiError(400, { status: 400, detail: 'Malformed JSON' })).message,
    ).toBe('Malformed JSON')
    expect(describeSubmitFailure(new ApiError(400, null)).message).toContain('Could not publish')
  })

  it('explains an invalid geometry', () => {
    expect(
      describeSubmitFailure(
        new ApiError(422, { status: 422, detail: 'Self-intersection at (1, 2)' }),
      ).message,
    ).toBe('The polygon is not valid: Self-intersection at (1, 2).')
    expect(describeSubmitFailure(new ApiError(422, null)).message).toBe(
      'The polygon is not valid: unknown reason.',
    )
  })

  it('falls back to a generic message for server and network errors', () => {
    expect(describeSubmitFailure(new ApiError(500, null)).message).toContain('Could not publish')
    expect(describeSubmitFailure(new TypeError('Failed to fetch')).message).toContain(
      'Could not publish',
    )
  })
})
