import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import type { AuthResponse } from '../../api/types'
import { server } from '../../test/server'
import { TestProviders } from '../../test/providers'
import { TEST_USER } from '../../test/session'
import { AuthDialog } from './AuthDialog'

const RESPONSE: AuthResponse = {
  accessToken: 'token',
  tokenType: 'Bearer',
  expiresIn: 7200,
  user: TEST_USER,
}

function problem(status: number, body: object) {
  return HttpResponse.json(
    { status, ...body },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

function renderDialog() {
  const onClose = vi.fn()
  const onAuthenticated = vi.fn()
  render(
    <TestProviders>
      <AuthDialog
        reason="Log in to publish a lot."
        onClose={onClose}
        onAuthenticated={onAuthenticated}
      />
    </TestProviders>,
  )
  return { onClose, onAuthenticated }
}

describe('AuthDialog', () => {
  it('logs in and reports success', async () => {
    server.use(http.post('*/api/auth/login', () => HttpResponse.json(RESPONSE)))
    const user = userEvent.setup()
    const { onAuthenticated } = renderDialog()
    expect(screen.getByRole('dialog', { name: 'Log in' })).toHaveTextContent(
      'Log in to publish a lot.',
    )

    await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Welcome back, Kaio')).toBeInTheDocument()
    expect(onAuthenticated).toHaveBeenCalledTimes(1)
  })

  it('shows invalid credentials above the form', async () => {
    server.use(
      http.post('*/api/auth/login', () => problem(401, { detail: 'Invalid email or password.' })),
    )
    const user = userEvent.setup()
    const { onAuthenticated } = renderDialog()

    await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
    await user.type(screen.getByLabelText('Password'), 'wrong')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
    expect(onAuthenticated).not.toHaveBeenCalled()
  })

  it('checks the fields before calling the server', async () => {
    const user = userEvent.setup()
    renderDialog()

    await user.click(screen.getByRole('button', { name: 'Log in' }))

    expect(screen.getByText('Enter a valid email')).toBeInTheDocument()
    expect(screen.getByText('Enter your password')).toBeInTheDocument()
  })

  it('creates an account', async () => {
    let body: unknown
    server.use(
      http.post('*/api/auth/register', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(RESPONSE, { status: 201 })
      }),
    )
    const user = userEvent.setup()
    const { onAuthenticated } = renderDialog()

    await user.click(screen.getByRole('tab', { name: 'Create account' }))
    await user.type(screen.getByLabelText('Name'), 'Kaio')
    await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByText('Welcome, Kaio')).toBeInTheDocument()
    expect(body).toEqual({ name: 'Kaio', email: 'kaio@example.com', password: 'secret123' })
    expect(onAuthenticated).toHaveBeenCalledTimes(1)
  })

  it('rejects a weak password without calling the server', async () => {
    const user = userEvent.setup()
    renderDialog()

    await user.click(screen.getByRole('tab', { name: 'Create account' }))
    expect(screen.getByText('8 to 64 characters, with a letter and a digit')).toBeInTheDocument()
    await user.type(screen.getByLabelText('Name'), 'Kaio')
    await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
    await user.type(screen.getByLabelText('Password'), 'password')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(screen.getByText('Include at least one digit')).toBeInTheDocument()
  })

  it('shows a taken email under the email field', async () => {
    server.use(
      http.post('*/api/auth/register', () =>
        problem(409, { detail: 'An account with this email already exists.' }),
      ),
    )
    const user = userEvent.setup()
    const { onAuthenticated } = renderDialog()

    await user.click(screen.getByRole('tab', { name: 'Create account' }))
    await user.type(screen.getByLabelText('Name'), 'Kaio')
    await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(
      await screen.findByText('An account with this email already exists.'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(onAuthenticated).not.toHaveBeenCalled()
  })

  it('closes with Escape, the close button and a click on the backdrop', async () => {
    const user = userEvent.setup()
    const { onClose } = renderDialog()

    await user.keyboard('{Escape}')
    await user.click(screen.getByRole('button', { name: 'Close' }))
    await user.click(screen.getByRole('dialog').parentElement!)
    // A click inside the dialog does not close it
    await user.click(screen.getByRole('dialog'))

    expect(onClose).toHaveBeenCalledTimes(3)
  })

  it('closes after logging in when nobody waits for the login', async () => {
    server.use(http.post('*/api/auth/login', () => HttpResponse.json(RESPONSE)))
    const user = userEvent.setup()
    const onClose = vi.fn()
    render(
      <TestProviders>
        <AuthDialog onClose={onClose} />
      </TestProviders>,
    )

    await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
    await user.type(screen.getByLabelText('Password'), 'secret123')
    await user.click(screen.getByRole('button', { name: 'Log in' }))

    await vi.waitFor(() => expect(onClose).toHaveBeenCalledTimes(1))
  })
})
