import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { LandLotFilters } from '../../api/types'
import { FiltersPanel } from './FiltersPanel'

function renderPanel(filters: LandLotFilters = {}) {
  const onChange = vi.fn()
  render(<FiltersPanel filters={filters} onChange={onChange} />)
  return { onChange }
}

describe('FiltersPanel', () => {
  it('shows how many filters are active', () => {
    renderPanel({ maxPrice: 100, status: 'SOLD' })

    expect(screen.getByRole('button', { name: 'Filters (2)' })).toHaveAttribute(
      'aria-expanded',
      'false',
    )
  })

  it('opens with the filters in use and applies the edited ones', async () => {
    const user = userEvent.setup()
    const { onChange } = renderPanel({ maxPrice: 500000 })

    await user.click(screen.getByRole('button', { name: 'Filters (1)' }))
    expect(screen.getByLabelText('Max price')).toHaveValue('500000')
    await user.type(screen.getByLabelText('Min area'), '300')
    await user.selectOptions(screen.getByLabelText('Status'), 'AVAILABLE')
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    expect(onChange).toHaveBeenCalledWith({ maxPrice: 500000, minArea: 300, status: 'AVAILABLE' })
    expect(screen.queryByRole('form', { name: 'Filters' })).not.toBeInTheDocument()
  })

  it('keeps the form open and explains an inverted range', async () => {
    const user = userEvent.setup()
    const { onChange } = renderPanel()

    await user.click(screen.getByRole('button', { name: 'Filters' }))
    await user.type(screen.getByLabelText('Min price'), '500')
    await user.type(screen.getByLabelText('Max price'), '100')
    await user.click(screen.getByRole('button', { name: 'Apply' }))

    expect(screen.getByText('Must not be below the minimum price')).toBeInTheDocument()
    expect(onChange).not.toHaveBeenCalled()
  })

  it('clears every filter', async () => {
    const user = userEvent.setup()
    const { onChange } = renderPanel({ minPrice: 1, status: 'SOLD' })

    await user.click(screen.getByRole('button', { name: 'Filters (2)' }))
    await user.click(screen.getByRole('button', { name: 'Clear' }))

    expect(onChange).toHaveBeenCalledWith({})
  })

  it('discards an edit that was not applied', async () => {
    const user = userEvent.setup()
    renderPanel()

    await user.click(screen.getByRole('button', { name: 'Filters' }))
    await user.type(screen.getByLabelText('Min price'), '999')
    await user.click(screen.getByRole('button', { name: 'Filters' }))
    await user.click(screen.getByRole('button', { name: 'Filters' }))

    expect(screen.getByLabelText('Min price')).toHaveValue('')
  })
})
