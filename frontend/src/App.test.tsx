import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'
import { TestProviders } from './test/providers'

describe('App', () => {
  it('shows the map with the tools and the base layer switcher', () => {
    const { container } = render(
      <TestProviders>
        <App />
      </TestProviders>,
    )

    expect(container.querySelector('.ol-viewport')).not.toBeNull()
    expect(screen.getByRole('toolbar', { name: 'Map tools' })).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Base layer' })).toBeInTheDocument()
  })
})
