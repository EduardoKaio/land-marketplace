import { requestJson } from './http'
import type { AuthResponse, LoginRequest, RegisterRequest } from './types'

/** Creates the account and logs it in. Rejects with ApiError 400 or 409 (email taken). */
export function register(request: RegisterRequest): Promise<AuthResponse> {
  return requestJson('/api/auth/register', { method: 'POST', body: request })
}

/** Rejects with ApiError 401 for an unknown email or a wrong password. */
export function login(request: LoginRequest): Promise<AuthResponse> {
  return requestJson('/api/auth/login', { method: 'POST', body: request })
}
