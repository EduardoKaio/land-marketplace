import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import type OlMap from 'ol/Map'
import { describe, expect, it, vi } from 'vitest'
import type { LandLotFeature } from '../../api/types'
import {
  createLandLotSource,
  readLandLotFeatures,
  type LandLotSource,
} from '../../map/landLotLayer'
import { useMap } from '../../map/MapContext'
import { MapView } from '../../map/MapView'
import { landLotFeature } from '../../test/fixtures'
import { TestProviders } from '../../test/providers'
import { signIn } from '../../test/session'
import { server } from '../../test/server'
import { MyLotsPanel } from './MyLotsPanel'

const MINE = landLotFeature()
const SOLD: LandLotFeature = {
  ...landLotFeature({ id: 'sold-lot' }),
  properties: { ...landLotFeature().properties, title: 'Sold lot', status: 'SOLD' },
}

function serveMyLots(...lots: LandLotFeature[]) {
  server.use(
    http.get('*/api/me/land-lots', () =>
      HttpResponse.json({ type: 'FeatureCollection', features: lots, truncated: false }),
    ),
  )
}

function renderPanel() {
  signIn()
  const lotSource: LandLotSource = createLandLotSource()
  lotSource.addFeatures(readLandLotFeatures(MINE))
  const onClose = vi.fn()
  const maps: OlMap[] = []
  function Capture() {
    maps.push(useMap())
    return null
  }
  render(
    <TestProviders>
      <MapView>
        <MyLotsPanel lotSource={lotSource} onClose={onClose} />
        <Capture />
      </MapView>
    </TestProviders>,
  )
  return { lotSource, onClose, map: maps[0] }
}

describe('MyLotsPanel', () => {
  it('lists the lots of the user with price, area and status', async () => {
    serveMyLots(MINE, SOLD)
    renderPanel()

    const items = await screen.findAllByRole('listitem')

    expect(items).toHaveLength(2)
    expect(items[0]).toHaveTextContent('Corner lot')
    expect(items[0]).toHaveTextContent('R$250,000.00 · 1.22 ha')
    expect(items[1]).toHaveTextContent('Sold')
  })

  it('invites to draw the first lot when there is none', async () => {
    serveMyLots()
    renderPanel()

    expect(await screen.findByText(/You have no lots yet/)).toBeInTheDocument()
  })

  it('says when the lots could not be loaded', async () => {
    server.use(http.get('*/api/me/land-lots', () => new HttpResponse(null, { status: 503 })))
    renderPanel()

    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load your lots')
  })

  it('zooms the map onto a lot', async () => {
    serveMyLots(MINE)
    const { map } = renderPanel()
    const fit = vi.spyOn(map.getView(), 'fit').mockImplementation(() => {})

    await userEvent.click(await screen.findByRole('button', { name: 'Zoom' }))

    const [extent, options] = fit.mock.calls[0]
    expect(extent).toEqual(readLandLotFeatures(MINE)[0].getGeometry()!.getExtent())
    expect(options).toMatchObject({ maxZoom: 18 })
  })

  it('deletes a lot after confirmation and removes it from the map', async () => {
    serveMyLots(MINE)
    let deletedId = ''
    server.use(
      http.delete('*/api/land-lots/:id', ({ params }) => {
        deletedId = String(params.id)
        serveMyLots()
        return new HttpResponse(null, { status: 204 })
      }),
    )
    const user = userEvent.setup()
    const { lotSource } = renderPanel()

    await user.click(await screen.findByRole('button', { name: 'Delete' }))
    const confirmation = screen.getByRole('group', { name: 'Confirm deletion' })
    expect(confirmation).toHaveTextContent('Delete this lot?')
    await user.click(within(confirmation).getByRole('button', { name: 'Yes, delete' }))

    expect(await screen.findByText('Lot deleted')).toBeInTheDocument()
    expect(deletedId).toBe(MINE.id)
    expect(lotSource.getFeatureById(MINE.id)).toBeNull()
    expect(await screen.findByText(/You have no lots yet/)).toBeInTheDocument()
  })

  it('keeps the lot when the user changes their mind', async () => {
    serveMyLots(MINE)
    const user = userEvent.setup()
    renderPanel()

    await user.click(await screen.findByRole('button', { name: 'Delete' }))
    await user.click(screen.getByRole('button', { name: 'Keep' }))

    expect(screen.getByRole('button', { name: 'Delete' })).toBeInTheDocument()
  })

  it('shows why a deletion failed', async () => {
    serveMyLots(MINE)
    server.use(
      http.delete('*/api/land-lots/:id', () =>
        HttpResponse.json(
          { status: 403, detail: 'Only the owner can change or delete this land lot.' },
          { status: 403, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )
    const user = userEvent.setup()
    const { lotSource } = renderPanel()

    await user.click(await screen.findByRole('button', { name: 'Delete' }))
    await user.click(screen.getByRole('button', { name: 'Yes, delete' }))

    expect(
      await screen.findByText('Only the owner can change or delete this land lot.'),
    ).toBeInTheDocument()
    expect(lotSource.getFeatureById(MINE.id)).not.toBeNull()
  })

  it('closes', async () => {
    serveMyLots()
    const { onClose } = renderPanel()

    await userEvent.click(screen.getByRole('button', { name: 'Close' }))

    expect(onClose).toHaveBeenCalled()
  })
})
