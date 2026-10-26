import { useForm, useWatch, type FieldError } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import type Polygon from 'ol/geom/Polygon'
import type { ReactNode } from 'react'
import { createLandLot } from '../../api/landLots'
import { MY_LOTS_KEY } from '../../api/queryKeys'
import type { LandLotFeature } from '../../api/types'
import { formatArea, formatCurrency, pricePerSquareMeter } from '../../shared/format'
import { maskPhone } from '../../shared/phone'
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
    trigger,
    control,
    formState: { errors },
  } = useForm<CreateLandLotFormValues>({
    resolver: zodResolver(createLandLotSchema),
    defaultValues: EMPTY_FORM,
    // Errors show up when the user leaves a field, not only on submit, and then follow each key
    mode: 'onTouched',
  })
  const phone = register('contact.phone')
  const email = register('contact.email')
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: createLandLot,
    // The new lot belongs in "My lots" too
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MY_LOTS_KEY }),
  })
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
            <input
              id="lot-contact-phone"
              type="tel"
              autoComplete="tel-national"
              placeholder="(84) 99999-8888"
              {...phone}
              // Masks the input before react-hook-form reads it, so the form holds the masked text
              onChange={(event) => {
                event.target.value = maskPhone(event.target.value)
                return phone.onChange(event)
              }}
            />
          </Field>
          <Field id="lot-contact-email" label="Email" error={errors.contact?.email}>
            <input
              id="lot-contact-email"
              type="email"
              autoComplete="email"
              {...email}
              // "Enter a phone or an email" sits under the phone, so typing an email must check the
              // phone again. Not the deps option: in onTouched mode it waits for the email to be
              // touched, and the error would stay while the first email is typed
              onChange={async (event) => {
                await email.onChange(event)
                if (errors.contact?.phone) {
                  await trigger('contact.phone')
                }
              }}
            />
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
