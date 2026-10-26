import { AuthProvider } from './features/auth/AuthProvider'
import { MapWorkspace } from './features/map-workspace/MapWorkspace'
import { MapView } from './map/MapView'
import { ToastProvider } from './shared/ui/toast/ToastProvider'

export default function App() {
  return (
    <ToastProvider>
      {/* Inside the toasts: an expired session is announced with one */}
      <AuthProvider>
        <main className="app">
          <MapView>
            <MapWorkspace />
          </MapView>
        </main>
      </AuthProvider>
    </ToastProvider>
  )
}
