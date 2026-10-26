import type { AuthResponse, User } from '../../api/types'

/**
 * The logged in session, kept in localStorage so a reload does not log the user out.
 *
 * localStorage instead of an httpOnly cookie: any script running on the page can read
 * localStorage, so an XSS flaw could steal the token, while an httpOnly cookie is out of reach of
 * scripts. But the browser sends cookies by itself, which brings CSRF back (SameSite plus a CSRF
 * token), and the API would have to issue the cookie and accept credentialed CORS. The token here
 * is short lived (2 hours by default), React escapes every text it renders and the app never
 * injects raw HTML, which keeps the XSS surface small. A Content-Security-Policy would be the next
 * step.
 */
export interface Session {
  accessToken: string
  /** Epoch milliseconds. */
  expiresAt: number
  user: User
}

export const SESSION_KEY = 'land-marketplace.session'

export function sessionFrom(response: AuthResponse, now = Date.now()): Session {
  return {
    accessToken: response.accessToken,
    expiresAt: now + response.expiresIn * 1000,
    user: response.user,
  }
}

/** The stored session, or null when there is none, it expired or it cannot be read. */
export function readSession(storage: Storage = localStorage, now = Date.now()): Session | null {
  try {
    const raw = storage.getItem(SESSION_KEY)
    if (!raw) {
      return null
    }
    const session = JSON.parse(raw) as Session
    if (typeof session.accessToken !== 'string' || !(session.expiresAt > now)) {
      storage.removeItem(SESSION_KEY)
      return null
    }
    return session
  } catch {
    // Storage blocked (private mode) or a corrupted value: start logged out
    return null
  }
}

export function writeSession(session: Session, storage: Storage = localStorage): void {
  try {
    storage.setItem(SESSION_KEY, JSON.stringify(session))
  } catch {
    // Without storage the session still lives in memory until the page is reloaded
  }
}

export function clearSession(storage: Storage = localStorage): void {
  try {
    storage.removeItem(SESSION_KEY)
  } catch {
    // Nothing stored, nothing to clear
  }
}
