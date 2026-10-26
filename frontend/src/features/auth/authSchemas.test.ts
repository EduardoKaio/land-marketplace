import { describe, expect, it } from 'vitest'
import { ApiError } from '../../api/http'
import { describeAuthFailure, loginSchema, registerSchema } from './authSchemas'

function errorsOf(result: {
  success: boolean
  error?: { issues: { path: PropertyKey[]; message: string }[] }
}) {
  return result.error?.issues.map((issue) => `${String(issue.path[0])}: ${issue.message}`) ?? []
}

describe('registerSchema', () => {
  it('accepts a valid account and trims the text fields', () => {
    const result = registerSchema.safeParse({
      name: ' Kaio ',
      email: ' kaio@example.com ',
      password: 'secret123',
    })

    expect(result.success).toBe(true)
    expect(result.data).toEqual({ name: 'Kaio', email: 'kaio@example.com', password: 'secret123' })
  })

  it.each([
    ['short1', 'password: Use at least 8 characters'],
    ['onlyletters', 'password: Include at least one digit'],
    ['1234567890', 'password: Include at least one letter'],
    ['a1'.repeat(33), 'password: Use at most 64 characters'],
  ])('rejects the password %s, like the server', (password, expected) => {
    const result = registerSchema.safeParse({ name: 'Kaio', email: 'kaio@example.com', password })

    expect(errorsOf(result)).toContain(expected)
  })

  it('requires a name and a valid email', () => {
    const result = registerSchema.safeParse({ name: ' ', email: 'nope', password: 'secret123' })

    expect(errorsOf(result)).toEqual(['name: Enter your name', 'email: Enter a valid email'])
  })
})

describe('loginSchema', () => {
  it('only checks that both fields are filled', () => {
    expect(errorsOf(loginSchema.safeParse({ email: 'kaio@example.com', password: '' }))).toEqual([
      'password: Enter your password',
    ])
  })
})

describe('describeAuthFailure', () => {
  it('puts a taken email under the email field', () => {
    const error = new ApiError(409, {
      status: 409,
      detail: 'An account with this email already exists.',
    })

    expect(describeAuthFailure(error)).toEqual({
      fieldErrors: [{ field: 'email', message: 'An account with this email already exists.' }],
      message: null,
    })
  })

  it('puts validation errors of known fields under them', () => {
    const error = new ApiError(400, {
      status: 400,
      errors: [
        { field: 'password', message: 'must contain at least one letter and one digit' },
        { field: 'unknown', message: 'ignored' },
      ],
    })

    expect(describeAuthFailure(error).fieldErrors).toEqual([
      { field: 'password', message: 'must contain at least one letter and one digit' },
    ])
  })

  it('shows invalid credentials above the form', () => {
    const error = new ApiError(401, { status: 401, detail: 'Invalid email or password.' })

    expect(describeAuthFailure(error)).toEqual({
      fieldErrors: [],
      message: 'Invalid email or password.',
    })
  })

  it('explains a network failure', () => {
    expect(describeAuthFailure(new TypeError('Failed to fetch')).message).toMatch(
      /reach the server/,
    )
  })
})
