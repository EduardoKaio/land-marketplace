import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { ToastContext, type ToastApi } from './ToastContext'
import './toast.css'

export const TOAST_DURATION_MS = 5000

interface Toast {
  id: number
  kind: 'success' | 'error'
  message: string
}

/** Short messages in a corner of the screen; each one disappears by itself after a few seconds. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const nextId = useRef(0)

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const show = useCallback(
    (kind: Toast['kind'], message: string) => {
      nextId.current += 1
      const id = nextId.current
      setToasts((current) => [...current, { id, kind, message }])
      setTimeout(() => dismiss(id), TOAST_DURATION_MS)
    },
    [dismiss],
  )

  const api = useMemo<ToastApi>(
    () => ({
      success: (message) => show('success', message),
      error: (message) => show('error', message),
    }),
    [show],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts">
        {toasts.map((toast) => (
          // Errors interrupt screen readers (alert); successes wait their turn (status)
          <div
            key={toast.id}
            role={toast.kind === 'error' ? 'alert' : 'status'}
            className={`toast toast--${toast.kind}`}
          >
            <span>{toast.message}</span>
            <button type="button" aria-label="Dismiss" onClick={() => dismiss(toast.id)}>
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
