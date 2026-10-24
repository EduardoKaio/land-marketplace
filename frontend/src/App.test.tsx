import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('shows the map with the base layer switcher', () => {
    const { container } = render(<App />)

    expect(container.querySelector('.ol-viewport')).not.toBeNull()
    expect(screen.getByRole('group', { name: 'Base layer' })).toBeInTheDocument()
  })
})
