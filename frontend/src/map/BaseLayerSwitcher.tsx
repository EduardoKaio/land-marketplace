import type { BaseLayerKey } from './baseLayers'
import { useBaseLayer } from './useBaseLayer'

const OPTIONS: { key: BaseLayerKey; label: string }[] = [
  { key: 'osm', label: 'Map' },
  { key: 'satellite', label: 'Satellite' },
]

export function BaseLayerSwitcher() {
  const [baseLayer, setBaseLayer] = useBaseLayer()

  return (
    <fieldset className="base-layer-switcher">
      <legend>Base layer</legend>
      {OPTIONS.map(({ key, label }) => (
        <label key={key}>
          <input
            type="radio"
            name="base-layer"
            value={key}
            checked={baseLayer === key}
            onChange={() => setBaseLayer(key)}
          />
          {label}
        </label>
      ))}
    </fieldset>
  )
}
