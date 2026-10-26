import type { User } from '../api/types'
import { writeSession } from '../features/auth/session'

export const TEST_USER: User = {
  id: 'b0000000-0000-4000-8000-000000000001',
  name: 'Kaio',
  email: 'kaio@example.com',
}

export const TEST_TOKEN = 'test-access-token'

/** Starts the next render logged in, the way a reload finds a stored session. */
export function signIn(user: User = TEST_USER): void {
  writeSession({ accessToken: TEST_TOKEN, expiresAt: Date.now() + 3_600_000, user })
}
