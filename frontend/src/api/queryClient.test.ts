import { describe, expect, it } from 'vitest'
import { ApiError } from './http'
import { createQueryClient, shouldRetry } from './queryClient'

describe('shouldRetry', () => {
  it.each([400, 404, 409, 422])(
    'does not retry a %i: the server would reject it again',
    (status) => {
      expect(shouldRetry(0, new ApiError(status, null))).toBe(false)
    },
  )

  it('retries server errors and network failures twice', () => {
    expect(shouldRetry(0, new ApiError(503, null))).toBe(true)
    expect(shouldRetry(1, new TypeError('Failed to fetch'))).toBe(true)
    expect(shouldRetry(2, new TypeError('Failed to fetch'))).toBe(false)
  })

  it('is the default retry policy of the query client', () => {
    expect(createQueryClient().getDefaultOptions().queries?.retry).toBe(shouldRetry)
  })
})
