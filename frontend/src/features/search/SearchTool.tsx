import { keepPreviousData, skipToken, useQuery } from '@tanstack/react-query'
import { ApiError } from '../../api/http'
import { searchLandLots } from '../../api/landLots'
import type { LandLotFeatureCollection, LandLotFilters } from '../../api/types'
import type { LandLotLayer } from '../../map/landLotLayer'
import { useResultLayer } from '../../map/useResultLayer'
import { countActiveFilters } from '../filters/filters'
import { LotPopup } from '../lot-popup/LotPopup'
import { useCircleDraw } from './useCircleDraw'
import './search.css'

interface SearchToolProps {
  /** The explore layer, hidden while the results are shown. */
  lotLayer: LandLotLayer
  filters: LandLotFilters
  onClear: () => void
  onClearFilters: () => void
}

/** Search mode: rendered only while the mode is active, so unmounting removes circle and results. */
export function SearchTool({ lotLayer, filters, onClear, onClearFilters }: SearchToolProps) {
  const circle = useCircleDraw()
  const search = useQuery({
    // The filters are part of the key: new filters search the same circle again
    queryKey: ['land-lots', 'search', circle, filters],
    // skipToken: no request until there is a circle. The API calls the ground radius "radius"
    queryFn: circle
      ? ({ signal }) =>
          searchLandLots(
            { lon: circle.lon, lat: circle.lat, radius: circle.radiusMeters },
            filters,
            signal,
          )
      : skipToken,
    // Keeps the current results on the map while the filtered ones load, instead of flashing empty
    placeholderData: keepPreviousData,
  })
  const resultLayer = useResultLayer(lotLayer, search.data)

  if (!circle) {
    return (
      <p className="map-hint" role="status">
        Press, drag and release to draw the search circle.
      </p>
    )
  }
  return (
    <>
      <section className="search-panel" aria-label="Search results">
        <div role="status">
          {search.isPending && <p>Searching…</p>}
          {search.isError && <p className="search-panel__error">{describeError(search.error)}</p>}
          {search.isSuccess && (
            <ResultSummary
              results={search.data}
              filtered={countActiveFilters(filters) > 0}
              onClearFilters={onClearFilters}
            />
          )}
        </div>
        <button type="button" onClick={onClear}>
          Clear search
        </button>
      </section>
      {search.isSuccess && <LotPopup layer={resultLayer} />}
    </>
  )
}

interface ResultSummaryProps {
  results: LandLotFeatureCollection
  filtered: boolean
  onClearFilters: () => void
}

function ResultSummary({ results, filtered, onClearFilters }: ResultSummaryProps) {
  const count = results.features.length
  if (count === 0 && filtered) {
    return (
      <>
        <p className="search-panel__count">No lots match the filters in this area</p>
        <button type="button" className="search-panel__link" onClick={onClearFilters}>
          Clear filters
        </button>
      </>
    )
  }
  if (count === 0) {
    return (
      <>
        <p className="search-panel__count">No lots in this area</p>
        <p>Try a larger circle or another part of the map.</p>
      </>
    )
  }
  return (
    <>
      <p className="search-panel__count">
        {count} {count === 1 ? 'lot' : 'lots'} found
      </p>
      {results.truncated && <p>More lots match. Draw a smaller circle to see all of them.</p>}
    </>
  )
}

/** The server explains its rejections (for example a radius above the maximum); others are generic. */
function describeError(error: Error): string {
  if (error instanceof ApiError && error.status < 500) {
    return error.message
  }
  return 'Could not search lots. Please try again.'
}
