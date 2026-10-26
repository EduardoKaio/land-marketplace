import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { server } from '../test/server'
import { ApiError, requestJson, setAccessToken, setUnauthorizedHandler } from './http'

const UNAUTHORIZED = () =>
  HttpResponse.json(
    { status: 401, detail: 'The access token is invalid or expired. Please log in again.' },
    { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
  )

function captureAuthorization(): () => string | null {
  let header: string | null = null
  server.use(
    http.get('*/api/ping', ({ request }) => {
      header = request.headers.get('Authorization')
      return HttpResponse.json({ ok: true })
    }),
  )
  return () => header
}

describe('requestJson interceptor', () => {
  it('sends no Authorization header while logged out', async () => {
    const authorization = captureAuthorization()

    await requestJson('/api/ping')

    expect(authorization()).toBeNull()
  })

  it('sends the access token as a bearer token', async () => {
    const authorization = captureAuthorization()
    setAccessToken('abc.def.ghi')

    await requestJson('/api/ping')

    expect(authorization()).toBe('Bearer abc.def.ghi')
  })

  it('reports a 401 to a request that carried a token, as an expired session', async () => {
    server.use(http.get('*/api/me/land-lots', UNAUTHORIZED))
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    setAccessToken('expired')

    await expect(requestJson('/api/me/land-lots')).rejects.toBeInstanceOf(ApiError)

    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  // A wrong password also answers 401; that is a failed login, not an expired session
  it('leaves a 401 without token to the caller', async () => {
    server.use(http.post('*/api/auth/login', UNAUTHORIZED))
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)

    await expect(
      requestJson('/api/auth/login', { method: 'POST', body: {} }),
    ).rejects.toMatchObject({ status: 401 })

    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('does not report other errors as an expired session', async () => {
    server.use(http.delete('*/api/land-lots/1', () => new HttpResponse(null, { status: 403 })))
    const onUnauthorized = vi.fn()
    setUnauthorizedHandler(onUnauthorized)
    setAccessToken('valid')

    await expect(requestJson('/api/land-lots/1', { method: 'DELETE' })).rejects.toMatchObject({
      status: 403,
    })

    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('resolves a 204 without parsing a body', async () => {
    server.use(http.delete('*/api/land-lots/1', () => new HttpResponse(null, { status: 204 })))

    await expect(requestJson('/api/land-lots/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
