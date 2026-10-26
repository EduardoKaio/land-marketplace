import { describe, expect, it, vi } from 'vitest'
import { TEST_USER } from '../../test/session'
import { SESSION_KEY, clearSession, readSession, sessionFrom, writeSession } from './session'

const NOW = 1_800_000_000_000

describe('session', () => {
  it('expires after the lifetime the server gave the token', () => {
    const session = sessionFrom(
      { accessToken: 't', tokenType: 'Bearer', expiresIn: 7200, user: TEST_USER },
      NOW,
    )

    expect(session).toEqual({ accessToken: 't', expiresAt: NOW + 7_200_000, user: TEST_USER })
  })

  it('reads back what was written', () => {
    const session = { accessToken: 't', expiresAt: NOW + 1000, user: TEST_USER }
    writeSession(session)

    expect(readSession(localStorage, NOW)).toEqual(session)
  })

  it('discards an expired session', () => {
    writeSession({ accessToken: 't', expiresAt: NOW, user: TEST_USER })

    expect(readSession(localStorage, NOW)).toBeNull()
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('ignores a value it cannot understand', () => {
    localStorage.setItem(SESSION_KEY, '{not json')
    expect(readSession(localStorage, NOW)).toBeNull()

    localStorage.setItem(SESSION_KEY, JSON.stringify({ expiresAt: NOW + 1000 }))
    expect(readSession(localStorage, NOW)).toBeNull()
  })

  it('is empty after clearing', () => {
    writeSession({ accessToken: 't', expiresAt: NOW + 1000, user: TEST_USER })

    clearSession()

    expect(readSession(localStorage, NOW)).toBeNull()
  })

  // Private browsing modes can make every storage call throw
  it('keeps working when the storage is blocked', () => {
    const blocked = {
      getItem: vi.fn(() => {
        throw new Error('blocked')
      }),
      setItem: vi.fn(() => {
        throw new Error('blocked')
      }),
      removeItem: vi.fn(() => {
        throw new Error('blocked')
      }),
    } as unknown as Storage

    expect(readSession(blocked, NOW)).toBeNull()
    expect(() =>
      writeSession({ accessToken: 't', expiresAt: NOW, user: TEST_USER }, blocked),
    ).not.toThrow()
    expect(() => clearSession(blocked)).not.toThrow()
  })
})
