import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type OlMap from 'ol/Map'
import { describe, expect, it } from 'vitest'
import { BASE_LAYER_PROPERTY } from './baseLayers'
import { BaseLayerSwitcher } from './BaseLayerSwitcher'
import { useMap } from './MapContext'
import { MapView } from './MapView'

function visibleBaseLayers(map: OlMap): string[] {
  return map
    .getLayers()
    .getArray()
    .filter((layer) => layer.getVisible())
    .map((layer) => layer.get(BASE_LAYER_PROPERTY) as string)
}

function renderSwitcher() {
  const maps: OlMap[] = []
  function Capture() {
    maps.push(useMap())
    return null
  }
  render(
    <MapView>
      <BaseLayerSwitcher />
      <Capture />
    </MapView>,
  )
  return () => maps[0]
}

describe('BaseLayerSwitcher', () => {
  it('starts on the street map', () => {
    const map = renderSwitcher()

    expect(screen.getByRole('radio', { name: 'Map' })).toBeChecked()
    expect(screen.getByRole('radio', { name: 'Satellite' })).not.toBeChecked()
    expect(visibleBaseLayers(map())).toEqual(['osm'])
  })

  it('switches to satellite and back', async () => {
    const user = userEvent.setup()
    const map = renderSwitcher()

    await user.click(screen.getByRole('radio', { name: 'Satellite' }))

    expect(screen.getByRole('radio', { name: 'Satellite' })).toBeChecked()
    expect(visibleBaseLayers(map())).toEqual(['satellite'])

    await user.click(screen.getByRole('radio', { name: 'Map' }))

    expect(visibleBaseLayers(map())).toEqual(['osm'])
  })

  it('is labelled as a group for assistive technology', () => {
    renderSwitcher()

    expect(screen.getByRole('group', { name: 'Base layer' })).toBeInTheDocument()
  })
})
