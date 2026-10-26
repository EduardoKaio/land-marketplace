import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // The browser calls /api on the dev server, which forwards to the backend: same origin, so
      // no CORS in development, and the code never hardcodes the backend address
      '/api': process.env.API_PROXY_TARGET ?? 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: [
        // Only mounts the React tree into index.html
        'src/main.tsx',
        // Test setup and helpers, not application code
        'src/test/**',
        '**/*.test.{ts,tsx}',
        // Type declarations only: erased at compile time, so there is nothing to execute or cover
        '**/*.d.ts',
        'src/api/types.ts',
      ],
      // Above the project minimum of 80%, so a small drop fails the build before it gets there
      thresholds: {
        lines: 85,
        functions: 85,
        branches: 85,
        statements: 85,
      },
    },
  },
})
