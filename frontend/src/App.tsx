import { BaseLayerSwitcher } from './map/BaseLayerSwitcher'
import { MapView } from './map/MapView'

export default function App() {
  return (
    <main className="app">
      <MapView>
        <BaseLayerSwitcher />
      </MapView>
    </main>
  )
}
