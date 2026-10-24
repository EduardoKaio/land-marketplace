import { useForm, useWatch, type FieldError } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import type Polygon from 'ol/geom/Polygon'
import type { ReactNode } from 'react'
import { createLandLot } from '../../api/landLots'
import type { LandLotFeature } from '../../api/types'
import { formatArea, formatCurrency, pricePerSquareMeter } from '../../shared/format'
import { useToast } from '../../shared/ui/toast/ToastContext'
import {
  EMPTY_FORM,
  createLandLotSchema,
  toCreateLandLotRequest,
  type CreateLandLotFormValues,
} from './createLandLotSchema'
import { toApiGeometry } from './geometry'
import { describeSubmitFailure } from './serverErrors'

interface CreateLandLotPanelProps {
  /** Drawn polygon in the map projection. */
  draft: Polygon
  areaM2: number
  onCreated: (lot: LandLotFeature) => void
  onConflict: (conflictingLotIds: string[]) => void
  onCancel: () => void
}

export function CreateLandLotPanel({
  draft,
  areaM2,
  onCreated,
  onConflict,
  onCancel,
}: CreateLandLotPanelProps) {
  const toast = useToast()
  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<CreateLandLotFormValues>({
    resolver: zodResolver(createLandLotSchema),
    defaultValues: EMPTY_FORM,
  })
  const mutation = useMutation({ mutationFn: createLandLot })
  const price = useWatch({ control, name: 'price' })
  const perSquareMeter = pricePerSquareMeter(price, areaM2)

  const onSubmit = handleSubmit(async (values) => {
    try {
      // The only place the geometry leaves the map projection
      const lot = await mutation.mutateAsync(toCreateLandLotRequest(values, toApiGeometry(draft)))
      toast.success(`Lot "${lot.properties.title}" published`)
      onCreated(lot)
    } catch (error) {
      const failure = describeSubmitFailure(error)
      if (failure.conflictingLotIds.length > 0) {
        onConflict(failure.conflictingLotIds)
      }
      for (const { field, message } of failure.fieldErrors) {
        setError(field, { message })
      }
      if (failure.message) {
        toast.error(failure.message)
      }
    }
  })

  return (
    <aside className="lot-panel" aria-labelledby="lot-panel-title">
      <h2 id="lot-panel-title">New land lot</h2>
      <dl className="lot-panel__figures">
        <div>
          <dt>Area</dt>
          <dd>{formatArea(areaM2)}</dd>
        </div>
        <div>
          <dt>Price per m²</dt>
          <dd>{perSquareMeter === null ? 'Enter a price' : formatCurrency(perSquareMeter)}</dd>
        </div>
      </dl>
      <form onSubmit={onSubmit} noValidate>
        <Field id="lot-title" label="Title" error={errors.title}>
          <input id="lot-title" {...register('title')} />
        </Field>
        <Field id="lot-price" label="Total price (BRL)" error={errors.price}>
          <input
            id="lot-price"
            type="number"
            min="0"
            step="0.01"
            {...register('price', { valueAsNumber: true })}
          />
        </Field>
        <Field id="lot-description" label="Description" error={errors.description}>
          <textarea id="lot-description" rows={3} {...register('description')} />
        </Field>
        <fieldset>
          <legend>Contact</legend>
          <Field id="lot-contact-name" label="Name" error={errors.contact?.name}>
            <input id="lot-contact-name" {...register('contact.name')} />
          </Field>
          <Field id="lot-contact-phone" label="Phone" error={errors.contact?.phone}>
            <input id="lot-contact-phone" type="tel" {...register('contact.phone')} />
          </Field>
          <Field id="lot-contact-email" label="Email" error={errors.contact?.email}>
            <input id="lot-contact-email" type="email" {...register('contact.email')} />
          </Field>
        </fieldset>
        <div className="lot-panel__actions">
          <button type="button" onClick={onCancel}>
            Cancel
          </button>
          <button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Publishing…' : 'Publish lot'}
          </button>
        </div>
      </form>
    </aside>
  )
}

interface FieldProps {
  id: string
  label: string
  error?: FieldError
  children: ReactNode
}

function Field({ id, label, error, children }: FieldProps) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {children}
      {error?.message && (
        <p id={`${id}-error`} className="field__error">
          {error.message}
        </p>
      )}
    </div>
  )
}
