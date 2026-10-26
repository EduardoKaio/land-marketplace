import { useAuth } from './AuthContext'

interface AccountBarProps {
  onLogin: () => void
  onMyLots: () => void
  onLogout: () => void
}

/** Who is logged in, and the account actions. */
export function AccountBar({ onLogin, onMyLots, onLogout }: AccountBarProps) {
  const { user } = useAuth()
  if (!user) {
    return (
      <div className="account-bar">
        <button type="button" className="account-bar__primary" onClick={onLogin}>
          Log in
        </button>
      </div>
    )
  }
  return (
    <div className="account-bar">
      <span className="account-bar__name">{user.name}</span>
      <button type="button" onClick={onMyLots}>
        My lots
      </button>
      <button type="button" onClick={onLogout}>
        Log out
      </button>
    </div>
  )
}
