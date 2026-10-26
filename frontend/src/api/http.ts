import type { ProblemDetail } from './types'

const PROBLEM_JSON = 'application/problem+json'

/** A response with an error status, carrying the Problem Details body when the server sent one. */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null) {
    super(problem?.detail ?? problem?.title ?? `Request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  query?: Record<string, string | number>
  /** Serialized as JSON. */
  body?: unknown
  /** Lets the caller (for example React Query) cancel the request. */
  signal?: AbortSignal
}

// The interceptor state. Module level because every request goes through requestJson, and the
// AuthProvider is the only writer: it sets the token on login and registers the 401 handler
let accessToken: string | null = null
let onUnauthorized: () => void = () => {}

/** Token sent as "Authorization: Bearer" on every request; null sends none. */
export function setAccessToken(token: string | null): void {
  accessToken = token
}

/** Called when a request that carried a token gets 401: the session expired or was revoked. */
export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler
}

/**
 * Calls the API and parses the JSON response. Paths are relative to the page origin: the Vite
 * proxy (and later the web server) forwards /api to the backend.
 */
export async function requestJson<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const url = new URL(path, window.location.origin)
  for (const [name, value] of Object.entries(options.query ?? {})) {
    url.searchParams.set(name, String(value))
  }
  const headers = new Headers({ Accept: `application/json, ${PROBLEM_JSON}` })
  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }
  // Read once: the token may change while the request is in flight
  const token = accessToken
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(url, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal,
  })
  if (!response.ok) {
    // Only with a token: a 401 to a login with a wrong password is not an expired session
    if (response.status === 401 && token) {
      onUnauthorized()
    }
    throw new ApiError(response.status, await readProblem(response))
  }
  // 204 No Content (a delete) has no body to parse
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function readProblem(response: Response): Promise<ProblemDetail | null> {
  if (!response.headers.get('Content-Type')?.includes(PROBLEM_JSON)) {
    return null
  }
  try {
    return (await response.json()) as ProblemDetail
  } catch {
    return null
  }
}
