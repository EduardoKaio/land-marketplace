import { createContext, useContext } from 'react'
import type { LoginRequest, RegisterRequest, User } from '../../api/types'

export interface AuthApi {
  /** The logged in user, or null. */
  user: User | null
  login(request: LoginRequest): Promise<User>
  register(request: RegisterRequest): Promise<User>
  logout(): void
}

export const AuthContext = createContext<AuthApi | null>(null)

export function useAuth(): AuthApi {
  const auth = useContext(AuthContext)
  if (!auth) {
    throw new Error('useAuth must be used inside <AuthProvider>')
  }
  return auth
}
