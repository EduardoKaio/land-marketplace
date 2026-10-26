import { zodResolver } from '@hookform/resolvers/zod'
import { useState, type ReactNode } from 'react'
import { useForm, type FieldError } from 'react-hook-form'
import type { LandLotFilters } from '../../api/types'
import {
  countActiveFilters,
  filterFormSchema,
  toFilters,
  toFormValues,
  type FilterFormValues,
} from './filters'
import './filters.css'

interface FiltersPanelProps {
  filters: LandLotFilters
  onChange: (filters: LandLotFilters) => void
}

/** Opens a form over the map; Apply hands the filters to the map listing and the search. */
export function FiltersPanel({ filters, onChange }: FiltersPanelProps) {
  const [open, setOpen] = useState(false)
  const active = countActiveFilters(filters)

  return (
    <div className="filters">
      {open && (
        <FilterForm
          // A new form each time it opens, so it shows the filters in use, not an abandoned edit
          initial={filters}
          onApply={(applied) => {
            onChange(applied)
            setOpen(false)
          }}
          onClear={() => {
            onChange({})
            setOpen(false)
          }}
        />
      )}
      <button
        type="button"
        className="filters__toggle"
        aria-expanded={open}
        aria-controls="filters-form"
        onClick={() => setOpen(!open)}
      >
        {active > 0 ? `Filters (${active})` : 'Filters'}
      </button>
    </div>
  )
}

interface FilterFormProps {
  initial: LandLotFilters
  onApply: (filters: LandLotFilters) => void
  onClear: () => void
}

function FilterForm({ initial, onApply, onClear }: FilterFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FilterFormValues, unknown, LandLotFilters>({
    resolver: zodResolver(filterFormSchema.transform(toFilters)),
    defaultValues: toFormValues(initial),
  })

  return (
    <form
      id="filters-form"
      className="filters__form"
      aria-label="Filters"
      onSubmit={handleSubmit(onApply)}
      noValidate
    >
      <fieldset>
        <legend>Total price (BRL)</legend>
        <Range>
          <NumberField id="filter-min-price" label="Min price" error={errors.minPrice}>
            <input id="filter-min-price" inputMode="decimal" {...register('minPrice')} />
          </NumberField>
          <NumberField id="filter-max-price" label="Max price" error={errors.maxPrice}>
            <input id="filter-max-price" inputMode="decimal" {...register('maxPrice')} />
          </NumberField>
        </Range>
      </fieldset>
      <fieldset>
        <legend>Area (m²)</legend>
        <Range>
          <NumberField id="filter-min-area" label="Min area" error={errors.minArea}>
            <input id="filter-min-area" inputMode="decimal" {...register('minArea')} />
          </NumberField>
          <NumberField id="filter-max-area" label="Max area" error={errors.maxArea}>
            <input id="filter-max-area" inputMode="decimal" {...register('maxArea')} />
          </NumberField>
        </Range>
      </fieldset>
      <NumberField
        id="filter-max-price-per-m2"
        label="Max price per m² (BRL)"
        error={errors.maxPricePerSquareMeter}
      >
        <input
          id="filter-max-price-per-m2"
          inputMode="decimal"
          {...register('maxPricePerSquareMeter')}
        />
      </NumberField>
      <div className="field">
        <label htmlFor="filter-status">Status</label>
        <select id="filter-status" {...register('status')}>
          <option value="">Any</option>
          <option value="AVAILABLE">Available</option>
          <option value="SOLD">Sold</option>
        </select>
      </div>
      <div className="filters__actions">
        <button type="button" onClick={onClear}>
          Clear
        </button>
        <button type="submit">Apply</button>
      </div>
    </form>
  )
}

function Range({ children }: { children: ReactNode }) {
  return <div className="filters__range">{children}</div>
}

interface NumberFieldProps {
  id: string
  label: string
  error?: FieldError
  children: ReactNode
}

/** Text inputs with a numeric keyboard: type="number" would hide a typo as an empty value. */
function NumberField({ id, label, error, children }: NumberFieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
      {error?.message && <p className="field__error">{error.message}</p>}
    </div>
  )
}
