import { createPortal } from 'react-dom'
import type { LandLotProperties, LandLotStatus } from '../../api/types'
import type { LandLotLayer } from '../../map/landLotLayer'
import { formatArea, formatCurrency } from '../../shared/format'
import { useLotPopup } from './useLotPopup'
import './lotPopup.css'

const STATUS_LABELS: Record<LandLotStatus, string> = {
  AVAILABLE: 'Available',
  SOLD: 'Sold',
}

/** Details of the clicked lot of the layer, in a popup anchored on the map. */
export function LotPopup({ layer }: { layer: LandLotLayer }) {
  const { lot, container, close } = useLotPopup(layer)
  if (!lot) {
    return null
  }
  return createPortal(<LotDetails lot={lot} onClose={close} />, container)
}

function LotDetails({ lot, onClose }: { lot: LandLotProperties; onClose: () => void }) {
  const { contact } = lot
  return (
    <article className="lot-popup" role="dialog" aria-labelledby="lot-popup-title">
      <header className="lot-popup__header">
        <h3 id="lot-popup-title">{lot.title}</h3>
        <button type="button" className="lot-popup__close" aria-label="Close" onClick={onClose}>
          ×
        </button>
      </header>
      <span className={`lot-popup__status lot-popup__status--${lot.status.toLowerCase()}`}>
        {STATUS_LABELS[lot.status]}
      </span>
      <dl className="lot-popup__figures">
        <div>
          <dt>Price</dt>
          <dd>{formatCurrency(lot.price)}</dd>
        </div>
        <div>
          <dt>Area</dt>
          <dd>{formatArea(lot.areaSquareMeters)}</dd>
        </div>
        <div>
          <dt>Price per m²</dt>
          <dd>{formatCurrency(lot.pricePerSquareMeter)}</dd>
        </div>
      </dl>
      <p className="lot-popup__description">{lot.description}</p>
      <address className="lot-popup__contact">
        <strong>{contact.name}</strong>
        {contact.phone && <a href={`tel:${contact.phone}`}>{contact.phone}</a>}
        {contact.email && <a href={`mailto:${contact.email}`}>{contact.email}</a>}
      </address>
    </article>
  )
}
