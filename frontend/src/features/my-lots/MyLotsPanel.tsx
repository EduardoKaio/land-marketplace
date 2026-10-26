import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { deleteLandLot, listMyLandLots } from '../../api/landLots'
import { MY_LOTS_KEY } from '../../api/queryKeys'
import type { LandLotFeature } from '../../api/types'
import { lotExtent, type LandLotSource } from '../../map/landLotLayer'
import { useMap } from '../../map/MapContext'
import { formatArea, formatCurrency } from '../../shared/format'
import { useToast } from '../../shared/ui/toast/ToastContext'
import './myLots.css'

interface MyLotsPanelProps {
  /** The explore layer source: a deleted lot is removed from it too. */
  lotSource: LandLotSource
  onClose: () => void
}

/** The lots of the logged in account, with zoom and delete. */
export function MyLotsPanel({ lotSource, onClose }: MyLotsPanelProps) {
  const map = useMap()
  const toast = useToast()
  const queryClient = useQueryClient()
  const [confirmingId, setConfirmingId] = useState<string | null>(null)
  const lots = useQuery({ queryKey: MY_LOTS_KEY, queryFn: ({ signal }) => listMyLandLots(signal) })
  const removal = useMutation({
    mutationFn: deleteLandLot,
    onSuccess: (_result, id) => {
      const feature = lotSource.getFeatureById(id)
      if (feature) {
        lotSource.removeFeature(feature)
      }
      toast.success('Lot deleted')
      return queryClient.invalidateQueries({ queryKey: MY_LOTS_KEY })
    },
    onError: (error) => toast.error(error.message),
    onSettled: () => setConfirmingId(null),
  })

  const zoomTo = (lot: LandLotFeature) => {
    map.getView().fit(lotExtent(lot), { padding: [80, 80, 80, 80], maxZoom: 18, duration: 300 })
  }

  return (
    <aside className="my-lots" aria-labelledby="my-lots-title">
      <header className="my-lots__header">
        <h2 id="my-lots-title">My lots</h2>
        <button type="button" className="my-lots__close" aria-label="Close" onClick={onClose}>
          ×
        </button>
      </header>
      {lots.isPending && <p role="status">Loading…</p>}
      {lots.isError && (
        <p className="my-lots__error" role="alert">
          Could not load your lots. Try again in a moment.
        </p>
      )}
      {lots.isSuccess && lots.data.features.length === 0 && (
        <p className="my-lots__empty">
          You have no lots yet. Use <strong>New lot</strong> to draw your first one.
        </p>
      )}
      {lots.isSuccess && lots.data.features.length > 0 && (
        <ul className="my-lots__list">
          {lots.data.features.map((lot) => (
            <li key={lot.id} className="my-lots__item">
              <div className="my-lots__summary">
                <strong>{lot.properties.title}</strong>
                <span>
                  {formatCurrency(lot.properties.price)} ·{' '}
                  {formatArea(lot.properties.areaSquareMeters)}
                  {lot.properties.status === 'SOLD' && ' · Sold'}
                </span>
              </div>
              {confirmingId === lot.id ? (
                <div className="my-lots__actions" role="group" aria-label="Confirm deletion">
                  <span>Delete this lot?</span>
                  <button
                    type="button"
                    className="my-lots__danger"
                    disabled={removal.isPending}
                    onClick={() => removal.mutate(lot.id)}
                  >
                    {removal.isPending ? 'Deleting…' : 'Yes, delete'}
                  </button>
                  <button type="button" onClick={() => setConfirmingId(null)}>
                    Keep
                  </button>
                </div>
              ) : (
                <div className="my-lots__actions">
                  <button type="button" onClick={() => zoomTo(lot)}>
                    Zoom
                  </button>
                  <button type="button" onClick={() => setConfirmingId(lot.id)}>
                    Delete
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </aside>
  )
}
