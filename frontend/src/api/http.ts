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
  method?: 'GET' | 'POST'
  query?: Record<string, string | number>
  /** Serialized as JSON. */
  body?: unknown
  /** Lets the caller (for example React Query) cancel the request. */
  signal?: AbortSignal
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

  const response = await fetch(url, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal,
  })
  if (!response.ok) {
    throw new ApiError(response.status, await readProblem(response))
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
