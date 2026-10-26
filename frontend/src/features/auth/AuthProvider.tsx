import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../../api/auth'
import { setAccessToken, setUnauthorizedHandler } from '../../api/http'
import { MY_DATA_KEY } from '../../api/queryKeys'
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '../../api/types'
import { useToast } from '../../shared/ui/toast/ToastContext'
import { AuthContext, type AuthApi } from './AuthContext'
import { clearSession, readSession, sessionFrom, writeSession, type Session } from './session'

/**
 * Owns the session: keeps it in state and in localStorage, hands the token to the HTTP client and
 * logs out when the API answers 401 to a request that carried the token.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const toast = useToast()
  const queryClient = useQueryClient()
  const [session, setSession] = useState<Session | null>(() => readSession())

  // The HTTP client reads the token on each request; the effect keeps it in step with the state
  useEffect(() => {
    setAccessToken(session?.accessToken ?? null)
  }, [session])

  const logout = useCallback(() => {
    clearSession()
    setAccessToken(null)
    setSession(null)
    queryClient.removeQueries({ queryKey: MY_DATA_KEY })
  }, [queryClient])

  useEffect(() => {
    setUnauthorizedHandler(() => {
      logout()
      toast.error('Your session expired. Please log in again.')
    })
    return () => setUnauthorizedHandler(() => {})
  }, [logout, toast])

  const start = useCallback((response: AuthResponse): User => {
    const started = sessionFrom(response)
    writeSession(started)
    // Set at once, not only in the effect: the caller may send an authenticated request next
    setAccessToken(started.accessToken)
    setSession(started)
    return started.user
  }, [])

  const api = useMemo<AuthApi>(
    () => ({
      user: session?.user ?? null,
      login: async (request: LoginRequest) => start(await authApi.login(request)),
      register: async (request: RegisterRequest) => start(await authApi.register(request)),
      logout,
    }),
    [session, start, logout],
  )

  return <AuthContext.Provider value={api}>{children}</AuthContext.Provider>
}
