import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import type OlMap from 'ol/Map'
import Draw, { DrawEvent } from 'ol/interaction/Draw'
import Feature from 'ol/Feature'
import { act } from 'react'
import { describe, expect, it } from 'vitest'
import { useMap } from '../../map/MapContext'
import { MapView } from '../../map/MapView'
import { CONFLICT_PROPERTY, landLotStyle } from '../../map/landLotStyle'
import type VectorLayer from 'ol/layer/Vector'
import { landLotFeature, pontaNegraPolygon } from '../../test/fixtures'
import { TestProviders } from '../../test/providers'
import { server } from '../../test/server'
import { MapWorkspace } from './MapWorkspace'
import type { LandLotSource } from '../../map/landLotLayer'
import { readLandLotFeatures } from '../../map/landLotLayer'

function renderWorkspace() {
  const maps: OlMap[] = []
  function Capture() {
    maps.push(useMap())
    return null
  }
  render(
    <TestProviders>
      <MapView>
        <MapWorkspace />
        <Capture />
      </MapView>
    </TestProviders>,
  )
  const map = maps[0]
  const draw = () =>
    map
      .getInteractions()
      .getArray()
      .find((i) => i instanceof Draw) as Draw | undefined
  const lotSource = () => {
    const layer = map
      .getLayers()
      .getArray()
      .find((candidate) => (candidate as VectorLayer).getStyle?.() === landLotStyle)
    return (layer as VectorLayer<LandLotSource>).getSource()!
  }
  return { map, draw, lotSource }
}

function drawPolygon(draw: Draw) {
  act(() => {
    draw.dispatchEvent(new DrawEvent('drawend', new Feature(pontaNegraPolygon())))
  })
}

describe('MapWorkspace', () => {
  it('adds the drawing interactions only while drawing a lot', async () => {
    const user = userEvent.setup()
    const { draw } = renderWorkspace()
    expect(draw()).toBeUndefined()

    await user.click(screen.getByRole('button', { name: 'Draw lot' }))
    expect(draw()).toBeDefined()
    expect(screen.getByText(/Click to add vertices/)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Explore' }))
    expect(draw()).toBeUndefined()
  })

  it('opens the form when the polygon is finished and adds the published lot to the map', async () => {
    server.use(
      http.post('*/api/land-lots', () => HttpResponse.json(landLotFeature(), { status: 201 })),
    )
    const user = userEvent.setup()
    const { draw, lotSource } = renderWorkspace()
    await user.click(screen.getByRole('button', { name: 'Draw lot' }))

    drawPolygon(draw()!)
    await user.type(screen.getByLabelText('Title'), 'Corner lot')
    await user.type(screen.getByLabelText('Total price (BRL)'), '250000')
    await user.type(screen.getByLabelText('Description'), 'Flat terrain')
    await user.type(screen.getByLabelText('Name'), 'Kaio')
    await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
    await user.click(screen.getByRole('button', { name: 'Publish lot' }))

    expect(await screen.findByText('Lot "Corner lot" published')).toBeInTheDocument()
    expect(lotSource().getFeatureById(landLotFeature().id)).not.toBeNull()
    expect(screen.queryByRole('heading', { name: 'New land lot' })).not.toBeInTheDocument()
    expect(draw()!.getActive()).toBe(true)
  })

  it('marks the conflicting lots in red and clears the marks on cancel', async () => {
    server.use(
      http.post('*/api/land-lots', () =>
        HttpResponse.json(
          { status: 409, conflictingLotIds: ['existing'] },
          { status: 409, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const user = userEvent.setup()
    const { draw, lotSource } = renderWorkspace()
    const [existing] = readLandLotFeatures(landLotFeature({ id: 'existing' }))
    lotSource().addFeature(existing)
    await user.click(screen.getByRole('button', { name: 'Draw lot' }))

    drawPolygon(draw()!)
    await user.type(screen.getByLabelText('Title'), 'Overlapping lot')
    await user.type(screen.getByLabelText('Total price (BRL)'), '1000')
    await user.type(screen.getByLabelText('Description'), 'Desc')
    await user.type(screen.getByLabelText('Name'), 'Kaio')
    await user.type(screen.getByLabelText('Phone'), '123')
    await user.click(screen.getByRole('button', { name: 'Publish lot' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('overlaps 1 existing lot')
    expect(existing.get(CONFLICT_PROPERTY)).toBe(true)

    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(existing.get(CONFLICT_PROPERTY)).toBeUndefined()
    expect(screen.getByText(/Click to add vertices/)).toBeInTheDocument()
  })
})
