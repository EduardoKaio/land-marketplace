import { act, fireEvent, render, renderHook, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { useToast } from './ToastContext'
import { TOAST_DURATION_MS, ToastProvider } from './ToastProvider'

function Trigger() {
  const toast = useToast()
  return (
    <>
      <button onClick={() => toast.success('Saved')}>success</button>
      <button onClick={() => toast.error('Failed')}>error</button>
    </>
  )
}

afterEach(() => {
  vi.useRealTimers()
})

describe('ToastProvider', () => {
  it('announces successes politely and errors at once, then hides them', () => {
    vi.useFakeTimers()
    render(
      <ToastProvider>
        <Trigger />
      </ToastProvider>,
    )

    fireEvent.click(screen.getByText('success'))
    fireEvent.click(screen.getByText('error'))

    expect(screen.getByRole('status')).toHaveTextContent('Saved')
    expect(screen.getByRole('alert')).toHaveTextContent('Failed')

    act(() => vi.advanceTimersByTime(TOAST_DURATION_MS))

    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('can be dismissed before it expires', () => {
    render(
      <ToastProvider>
        <Trigger />
      </ToastProvider>,
    )
    fireEvent.click(screen.getByText('error'))

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }))

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})

describe('useToast', () => {
  it('fails clearly outside the provider', () => {
    expect(() => renderHook(() => useToast())).toThrow(
      'useToast must be used inside <ToastProvider>',
    )
  })
})
