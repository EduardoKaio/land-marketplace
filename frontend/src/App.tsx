import { MapWorkspace } from './features/map-workspace/MapWorkspace'
import { MapView } from './map/MapView'
import { ToastProvider } from './shared/ui/toast/ToastProvider'

export default function App() {
  return (
    <ToastProvider>
      <main className="app">
        <MapView>
          <MapWorkspace />
        </MapView>
      </main>
    </ToastProvider>
  )
}
