import { setupServer } from 'msw/node'

/** Mock API for tests; each test registers its handlers with server.use(...). */
export const server = setupServer()
