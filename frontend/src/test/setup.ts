import '@testing-library/jest-dom/vitest'
import 'vitest-canvas-mock'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { setAccessToken, setUnauthorizedHandler } from '../api/http'
import { server } from './server'

// jsdom has no ResizeObserver; OpenLayers uses it to follow the size of the map element
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver ??= ResizeObserverStub

// Any request without a handler fails the test instead of reaching the network
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterEach(() => {
  cleanup()
  server.resetHandlers()
  // The session lives in localStorage and the token in the HTTP module: both outlive a test
  localStorage.clear()
  setAccessToken(null)
  setUnauthorizedHandler(() => {})
})
afterAll(() => server.close())
