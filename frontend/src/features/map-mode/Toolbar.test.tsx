import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Toolbar } from './Toolbar'

describe('Toolbar', () => {
  it('marks the active tool as pressed', () => {
    render(<Toolbar mode="draw-lot" onSelect={() => {}} />)

    expect(screen.getByRole('button', { name: 'New lot' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Explore' })).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByRole('button', { name: 'Search' })).toHaveAttribute('aria-pressed', 'false')
  })

  it('reports the clicked tool', async () => {
    const onSelect = vi.fn()
    render(<Toolbar mode="explore" onSelect={onSelect} />)

    await userEvent.click(screen.getByRole('button', { name: 'Search' }))

    expect(onSelect).toHaveBeenCalledWith('search')
  })
})
