import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import type { CreateLandLotRequest } from '../../api/types'
import { PONTA_NEGRA_RING, landLotFeature, pontaNegraPolygon } from '../../test/fixtures'
import { TestProviders } from '../../test/providers'
import { server } from '../../test/server'
import { CreateLandLotPanel } from './CreateLandLotPanel'

const AREA_M2 = 12_246.04

function renderPanel() {
  const callbacks = { onCreated: vi.fn(), onConflict: vi.fn(), onCancel: vi.fn() }
  render(
    <TestProviders>
      <CreateLandLotPanel draft={pontaNegraPolygon()} areaM2={AREA_M2} {...callbacks} />
    </TestProviders>,
  )
  return callbacks
}

async function fillValidForm(user = userEvent.setup()) {
  await user.type(screen.getByLabelText('Title'), 'Corner lot')
  await user.type(screen.getByLabelText('Total price (BRL)'), '250000')
  await user.type(screen.getByLabelText('Description'), 'Flat terrain')
  await user.type(screen.getByLabelText('Name'), 'Kaio')
  await user.type(screen.getByLabelText('Email'), 'kaio@example.com')
  return user
}

function problem(status: number, body: object) {
  return HttpResponse.json(
    { status, ...body },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

describe('CreateLandLotPanel', () => {
  it('shows the area and the price per square meter as the price is typed', async () => {
    renderPanel()

    expect(screen.getByText('1.22 ha')).toBeInTheDocument()
    expect(screen.getByText('Enter a price')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Total price (BRL)'), '250000')

    expect(screen.getByText('R$20.41')).toBeInTheDocument()
  })

  it('publishes the lot with the geometry in WGS84 and reports success', async () => {
    let body: CreateLandLotRequest | undefined
    server.use(
      http.post('*/api/land-lots', async ({ request }) => {
        body = (await request.json()) as CreateLandLotRequest
        return HttpResponse.json(landLotFeature(), { status: 201 })
      }),
    )
    const { onCreated } = renderPanel()
    const user = await fillValidForm()

    await user.click(screen.getByRole('button', { name: 'Publish lot' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Lot "Corner lot" published')
    expect(onCreated).toHaveBeenCalledWith(landLotFeature())
    expect(body).toEqual({
      title: 'Corner lot',
      price: 250000,
      description: 'Flat terrain',
      contact: { name: 'Kaio', email: 'kaio@example.com' },
      geometry: { type: 'Polygon', coordinates: [PONTA_NEGRA_RING] },
    })
  })

  it('validates locally without calling the API', async () => {
    const request = vi.fn()
    server.use(http.post('*/api/land-lots', request))
    const { onCreated } = renderPanel()
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Name'), 'Kaio')

    await user.click(screen.getByRole('button', { name: 'Publish lot' }))

    expect(await screen.findByText('Enter a title')).toBeInTheDocument()
    expect(screen.getByText('Enter a description')).toBeInTheDocument()
    expect(screen.getAllByText('Enter a price')).toHaveLength(2)
    expect(screen.getByText('Enter a phone or an email')).toBeInTheDocument()
    expect(request).not.toHaveBeenCalled()
    expect(onCreated).not.toHaveBeenCalled()
  })

  it('explains an overlap and reports the conflicting lots', async () => {
    server.use(http.post('*/api/land-lots', () => problem(409, { conflictingLotIds: ['lot-a'] })))
    const { onConflict, onCreated } = renderPanel()
    const user = await fillValidForm()

    await user.click(screen.getByRole('button', { name: 'Publish lot' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'This area overlaps 1 existing lot. Adjust the polygon; sharing a border is allowed.',
    )
    expect(onConflict).toHaveBeenCalledWith(['lot-a'])
    expect(onCreated).not.toHaveBeenCalled()
  })

  it('shows server validation errors under the fields', async () => {
    server.use(
      http.post('*/api/land-lots', () =>
        problem(400, {
          errors: [{ field: 'contact.email', message: 'must be a well-formed email address' }],
        }),
      ),
    )
    renderPanel()
    const user = await fillValidForm()

    await user.click(screen.getByRole('button', { name: 'Publish lot' }))

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('reports an invalid geometry in a toast', async () => {
    server.use(
      http.post('*/api/land-lots', () => problem(422, { detail: 'Self-intersection at (1, 2)' })),
    )
    const { onConflict } = renderPanel()
    const user = await fillValidForm()

    await user.click(screen.getByRole('button', { name: 'Publish lot' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The polygon is not valid: Self-intersection at (1, 2).',
    )
    expect(onConflict).not.toHaveBeenCalled()
  })

  it('can be cancelled', async () => {
    const { onCancel } = renderPanel()

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(onCancel).toHaveBeenCalled()
  })
})
