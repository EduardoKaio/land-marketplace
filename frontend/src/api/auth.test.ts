import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { server } from '../test/server'
import { login, register } from './auth'
import type { AuthResponse } from './types'

const RESPONSE: AuthResponse = {
  accessToken: 'token',
  tokenType: 'Bearer',
  expiresIn: 7200,
  user: { id: 'u1', name: 'Kaio', email: 'kaio@example.com' },
}

describe('auth API client', () => {
  it('registers with name, email and password', async () => {
    let body: unknown
    server.use(
      http.post('*/api/auth/register', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(RESPONSE, { status: 201 })
      }),
    )

    await expect(
      register({ name: 'Kaio', email: 'kaio@example.com', password: 'secret123' }),
    ).resolves.toEqual(RESPONSE)
    expect(body).toEqual({ name: 'Kaio', email: 'kaio@example.com', password: 'secret123' })
  })

  it('logs in with email and password', async () => {
    let body: unknown
    server.use(
      http.post('*/api/auth/login', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(RESPONSE)
      }),
    )

    await expect(login({ email: 'kaio@example.com', password: 'secret123' })).resolves.toEqual(
      RESPONSE,
    )
    expect(body).toEqual({ email: 'kaio@example.com', password: 'secret123' })
  })
})
