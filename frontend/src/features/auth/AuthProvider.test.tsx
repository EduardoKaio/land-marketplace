import { useQuery } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { requestJson } from '../../api/http'
import { MY_LOTS_KEY } from '../../api/queryKeys'
import type { AuthResponse } from '../../api/types'
import { server } from '../../test/server'
import { TestProviders } from '../../test/providers'
import { signIn, TEST_TOKEN, TEST_USER } from '../../test/session'
import { useAuth } from './AuthContext'
import { readSession } from './session'

const RESPONSE: AuthResponse = {
  accessToken: 'fresh-token',
  tokenType: 'Bearer',
  expiresIn: 7200,
  user: TEST_USER,
}

/** Shows the auth state and exposes its actions as buttons. */
function Probe() {
  const auth = useAuth()
  const myLots = useQuery({ queryKey: MY_LOTS_KEY, queryFn: () => 'cached', enabled: !!auth.user })
  return (
    <>
      <p>{auth.user ? `Logged in as ${auth.user.name}` : 'Logged out'}</p>
      <p>{`My data: ${myLots.data ?? 'none'}`}</p>
      <button onClick={() => auth.login({ email: 'kaio@example.com', password: 'secret123' })}>
        login
      </button>
      <button
        onClick={() =>
          auth.register({ name: 'Kaio', email: 'k@example.com', password: 'secret123' })
        }
      >
        register
      </button>
      <button onClick={auth.logout}>logout</button>
    </>
  )
}

function renderProbe() {
  render(
    <TestProviders>
      <Probe />
    </TestProviders>,
  )
}

function captureAuthorization(): () => string | null {
  let header: string | null = null
  server.use(
    http.get('*/api/ping', ({ request }) => {
      header = request.headers.get('Authorization')
      return HttpResponse.json({})
    }),
  )
  return () => header
}

describe('AuthProvider', () => {
  it('starts logged out without a stored session', () => {
    renderProbe()

    expect(screen.getByText('Logged out')).toBeInTheDocument()
  })

  it('restores a stored session after a reload and sends its token', async () => {
    signIn()
    const authorization = captureAuthorization()

    renderProbe()
    await requestJson('/api/ping')

    expect(screen.getByText('Logged in as Kaio')).toBeInTheDocument()
    expect(authorization()).toBe(`Bearer ${TEST_TOKEN}`)
  })

  it('logs in, stores the session and sends the new token', async () => {
    server.use(http.post('*/api/auth/login', () => HttpResponse.json(RESPONSE)))
    const authorization = captureAuthorization()
    renderProbe()

    await userEvent.click(screen.getByRole('button', { name: 'login' }))

    expect(await screen.findByText('Logged in as Kaio')).toBeInTheDocument()
    expect(readSession()?.accessToken).toBe('fresh-token')
    await requestJson('/api/ping')
    expect(authorization()).toBe('Bearer fresh-token')
  })

  it('registers and starts the session at once', async () => {
    server.use(http.post('*/api/auth/register', () => HttpResponse.json(RESPONSE, { status: 201 })))
    renderProbe()

    await userEvent.click(screen.getByRole('button', { name: 'register' }))

    expect(await screen.findByText('Logged in as Kaio')).toBeInTheDocument()
  })

  it('logs out, forgets the token and drops the data of the user', async () => {
    signIn()
    const authorization = captureAuthorization()
    renderProbe()
    expect(await screen.findByText('My data: cached')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'logout' }))

    expect(screen.getByText('Logged out')).toBeInTheDocument()
    expect(screen.getByText('My data: none')).toBeInTheDocument()
    expect(readSession()).toBeNull()
    await requestJson('/api/ping')
    expect(authorization()).toBeNull()
  })

  it('logs out and warns when the API rejects the token', async () => {
    signIn()
    server.use(
      http.get('*/api/me/land-lots', () =>
        HttpResponse.json(
          { status: 401 },
          { status: 401, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    renderProbe()

    await act(() => requestJson('/api/me/land-lots').catch(() => {}))

    expect(screen.getByText('Logged out')).toBeInTheDocument()
    expect(screen.getByText('Your session expired. Please log in again.')).toBeInTheDocument()
  })
})
