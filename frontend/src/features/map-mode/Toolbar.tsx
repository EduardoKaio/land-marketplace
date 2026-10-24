import type { MapMode } from './mapMode'
import './toolbar.css'

const TOOLS: { mode: MapMode; label: string }[] = [
  { mode: 'explore', label: 'Explore' },
  { mode: 'draw-lot', label: 'Draw lot' },
  { mode: 'search', label: 'Search' },
]

interface ToolbarProps {
  mode: MapMode
  onSelect: (mode: MapMode) => void
}

export function Toolbar({ mode, onSelect }: ToolbarProps) {
  return (
    <div role="toolbar" aria-label="Map tools" className="map-toolbar">
      {TOOLS.map((tool) => (
        <button
          key={tool.mode}
          type="button"
          aria-pressed={mode === tool.mode}
          onClick={() => onSelect(tool.mode)}
        >
          {tool.label}
        </button>
      ))}
    </div>
  )
}
