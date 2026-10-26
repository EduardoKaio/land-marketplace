import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState, type ReactNode } from 'react'
import { useForm, type FieldError, type UseFormSetError } from 'react-hook-form'
import { useToast } from '../../shared/ui/toast/ToastContext'
import { useAuth } from './AuthContext'
import {
  describeAuthFailure,
  loginSchema,
  registerSchema,
  type LoginValues,
  type RegisterValues,
} from './authSchemas'
import './auth.css'

type Tab = 'login' | 'register'

interface AuthDialogProps {
  /** Why the dialog opened, for example "Log in to publish a lot". */
  reason?: string
  onClose: () => void
  /** Called once the user is logged in. */
  onAuthenticated?: () => void
}

/**
 * Log in or create an account. A plain modal div instead of <dialog>: jsdom has no showModal, and
 * the few behaviors needed (Escape, backdrop click, focus on open) are short to write.
 */
export function AuthDialog({ reason, onClose, onAuthenticated }: AuthDialogProps) {
  const [tab, setTab] = useState<Tab>('login')

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  const title = tab === 'login' ? 'Log in' : 'Create account'
  return (
    <div
      className="auth-backdrop"
      onMouseDown={(event) => event.target === event.currentTarget && onClose()}
    >
      <div className="auth-dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <header className="auth-dialog__header">
          <h2 id="auth-title">{title}</h2>
          <button type="button" className="auth-dialog__close" aria-label="Close" onClick={onClose}>
            ×
          </button>
        </header>
        {reason && <p className="auth-dialog__reason">{reason}</p>}
        <div className="auth-dialog__tabs" role="tablist">
          <TabButton tab="login" active={tab} onSelect={setTab}>
            Log in
          </TabButton>
          <TabButton tab="register" active={tab} onSelect={setTab}>
            Create account
          </TabButton>
        </div>
        {tab === 'login' ? (
          <LoginForm onDone={onAuthenticated ?? onClose} />
        ) : (
          <RegisterForm onDone={onAuthenticated ?? onClose} />
        )}
      </div>
    </div>
  )
}

function TabButton(props: {
  tab: Tab
  active: Tab
  onSelect: (tab: Tab) => void
  children: ReactNode
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={props.tab === props.active}
      onClick={() => props.onSelect(props.tab)}
    >
      {props.children}
    </button>
  )
}

function LoginForm({ onDone }: { onDone: () => void }) {
  const auth = useAuth()
  const toast = useToast()
  const [message, setMessage] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const onSubmit = handleSubmit(async (values) => {
    setMessage(null)
    try {
      const user = await auth.login(values)
      toast.success(`Welcome back, ${user.name}`)
      onDone()
    } catch (error) {
      setMessage(showFailure(error, setError))
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate>
      {message && (
        <p className="auth-dialog__error" role="alert">
          {message}
        </p>
      )}
      <Field id="login-email" label="Email" error={errors.email}>
        <input
          id="login-email"
          type="email"
          autoComplete="email"
          autoFocus
          {...register('email')}
        />
      </Field>
      <Field id="login-password" label="Password" error={errors.password}>
        <input
          id="login-password"
          type="password"
          autoComplete="current-password"
          {...register('password')}
        />
      </Field>
      <button type="submit" className="auth-dialog__submit" disabled={isSubmitting}>
        {isSubmitting ? 'Logging in…' : 'Log in'}
      </button>
    </form>
  )
}

function RegisterForm({ onDone }: { onDone: () => void }) {
  const auth = useAuth()
  const toast = useToast()
  const [message, setMessage] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { name: '', email: '', password: '' },
  })

  const onSubmit = handleSubmit(async (values) => {
    setMessage(null)
    try {
      const user = await auth.register(values)
      toast.success(`Welcome, ${user.name}`)
      onDone()
    } catch (error) {
      setMessage(showFailure(error, setError))
    }
  })

  return (
    <form onSubmit={onSubmit} noValidate>
      {message && (
        <p className="auth-dialog__error" role="alert">
          {message}
        </p>
      )}
      <Field id="register-name" label="Name" error={errors.name}>
        <input id="register-name" autoComplete="name" autoFocus {...register('name')} />
      </Field>
      <Field id="register-email" label="Email" error={errors.email}>
        <input id="register-email" type="email" autoComplete="email" {...register('email')} />
      </Field>
      <Field
        id="register-password"
        label="Password"
        hint="8 to 64 characters, with a letter and a digit"
        error={errors.password}
      >
        <input
          id="register-password"
          type="password"
          autoComplete="new-password"
          {...register('password')}
        />
      </Field>
      <button type="submit" className="auth-dialog__submit" disabled={isSubmitting}>
        {isSubmitting ? 'Creating account…' : 'Create account'}
      </button>
    </form>
  )
}

/** Puts server errors under their fields and returns the message that belongs to none. */
function showFailure<T extends LoginValues | RegisterValues>(
  error: unknown,
  setError: UseFormSetError<T>,
): string | null {
  const failure = describeAuthFailure(error)
  for (const { field, message } of failure.fieldErrors) {
    setError(field as Parameters<UseFormSetError<T>>[0], { message })
  }
  return failure.message
}

interface FieldProps {
  id: string
  label: string
  hint?: string
  error?: FieldError
  children: ReactNode
}

function Field({ id, label, hint, error, children }: FieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
      {hint && !error && <p className="field__hint">{hint}</p>}
      {error?.message && <p className="field__error">{error.message}</p>}
    </div>
  )
}
