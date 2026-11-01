import { zodResolver } from '@hookform/resolvers/zod'
import { MapPin, Plus, Trash2 } from 'lucide-react'
import { useEffect } from 'react'
import { useFieldArray, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

import type { LonLat } from '@/components/maps/geo'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useCreatePoint, useUpdatePoint } from '@/hooks/use-collection-points'
import { ApiRequestError } from '@/lib/api-client'
import type { CollectionPoint, WasteCategory } from '@/types/api'

import { isValidKg } from '@/lib/validation'

const inRange = (min: number, max: number) => (v: string) => {
  const n = Number(v)
  return v.trim() !== '' && Number.isFinite(n) && n >= min && n <= max
}

// Numeric inputs stay strings in the form and are converted on submit.
const schema = z
  .object({
    name: z.string().trim().min(1, 'Name is required').max(120, 'At most 120 characters'),
    longitude: z.string().refine(inRange(-180, 180), 'Longitude must be between -180 and 180'),
    latitude: z.string().refine(inRange(-90, 90), 'Latitude must be between -90 and 90'),
    demands: z.array(
      z.object({
        categoryId: z.string().min(1, 'Choose a category'),
        quantityKg: z.string().refine(isValidKg, 'Enter kg > 0 with at most 2 decimals'),
      }),
    ),
  })
  .superRefine((v, ctx) => {
    const seen = new Set<string>()
    v.demands.forEach((d, i) => {
      if (d.categoryId && seen.has(d.categoryId)) {
        ctx.addIssue({
          code: 'custom',
          path: ['demands', i, 'categoryId'],
          message: 'Category already used',
        })
      }
      seen.add(d.categoryId)
    })
  })
type FormValues = z.infer<typeof schema>

/** Create or edit panel; a card rather than a modal so the map stays clickable. */
export function PointForm({
  point,
  categories,
  pickOnMap,
  pickedLocation,
  onTogglePick,
  onDone,
}: {
  point: CollectionPoint | null
  categories: WasteCategory[]
  pickOnMap: boolean
  pickedLocation: LonLat | null
  onTogglePick: () => void
  onDone: (saved: CollectionPoint | null) => void
}) {
  const create = useCreatePoint()
  const update = useUpdatePoint()
  const activeCategories = categories.filter((c) => c.active)

  const {
    register,
    control,
    handleSubmit,
    setValue,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: point?.name ?? '',
      longitude: point ? String(point.longitude) : '',
      latitude: point ? String(point.latitude) : '',
      demands: point ? [] : [{ categoryId: '', quantityKg: '' }],
    },
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'demands' })
  const watchedDemands = useWatch({ control, name: 'demands' })

  // A map click while "Pick location" is on fills the coordinate fields.
  useEffect(() => {
    if (!pickedLocation) return
    setValue('longitude', pickedLocation.longitude.toFixed(6), { shouldValidate: true })
    setValue('latitude', pickedLocation.latitude.toFixed(6), { shouldValidate: true })
  }, [pickedLocation, setValue])

  const onError = (err: Error) => {
    if (err instanceof ApiRequestError && err.status === 400 && err.body?.fieldErrors) {
      for (const fe of err.body.fieldErrors) {
        setError(fe.field as keyof FormValues, { message: fe.message })
      }
    } else {
      toast.error(err.message)
    }
  }

  const onSubmit = (v: FormValues) => {
    const longitude = Number(v.longitude)
    const latitude = Number(v.latitude)
    if (point) {
      update.mutate(
        { id: point.id, body: { name: v.name, longitude, latitude } },
        {
          onSuccess: (saved) => {
            toast.success('Collection point updated')
            onDone(saved)
          },
          onError,
        },
      )
    } else {
      create.mutate(
        {
          name: v.name,
          longitude,
          latitude,
          demands: v.demands.map((d) => ({
            categoryId: d.categoryId,
            quantityKg: Number(d.quantityKg),
          })),
        },
        {
          onSuccess: (saved) => {
            toast.success('Collection point created')
            onDone(saved)
          },
          onError,
        },
      )
    }
  }

  const pending = create.isPending || update.isPending
  const demandsError = errors.demands?.root?.message ?? errors.demands?.message

  return (
    <Card>
      <CardHeader>
        <CardTitle>{point ? `Edit ${point.name}` : 'New collection point'}</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4" noValidate>
          <div className="flex flex-col gap-2">
            <Label htmlFor="point-name">Name</Label>
            <Input id="point-name" {...register('name')} />
            {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="flex flex-col gap-2">
              <Label htmlFor="point-longitude">Longitude</Label>
              <Input
                id="point-longitude"
                type="number"
                step="0.000001"
                {...register('longitude')}
              />
              {errors.longitude && (
                <p className="text-sm text-destructive">{errors.longitude.message}</p>
              )}
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="point-latitude">Latitude</Label>
              <Input id="point-latitude" type="number" step="0.000001" {...register('latitude')} />
              {errors.latitude && (
                <p className="text-sm text-destructive">{errors.latitude.message}</p>
              )}
            </div>
          </div>
          <div>
            <Button
              type="button"
              variant={pickOnMap ? 'default' : 'outline'}
              aria-pressed={pickOnMap}
              onClick={onTogglePick}
            >
              <MapPin />
              Pick location on map
            </Button>
            {pickOnMap && (
              <span className="ml-3 text-sm text-muted-foreground">
                Click the map to fill longitude and latitude.
              </span>
            )}
          </div>

          {!point && (
            <fieldset className="flex flex-col gap-2">
              <legend className="mb-1 text-sm font-medium">Demand (kg per waste category)</legend>
              {fields.map((f, i) => (
                <div key={f.id} className="flex flex-wrap items-start gap-2">
                  <div className="flex flex-col gap-1">
                    <Select
                      value={watchedDemands[i]?.categoryId ?? ''}
                      onValueChange={(v) =>
                        setValue(`demands.${i}.categoryId`, v, { shouldValidate: true })
                      }
                    >
                      <SelectTrigger className="w-44" aria-label="Waste category">
                        <SelectValue placeholder="Category" />
                      </SelectTrigger>
                      <SelectContent>
                        {activeCategories.map((c) => (
                          <SelectItem key={c.id} value={c.id}>
                            {c.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    {errors.demands?.[i]?.categoryId && (
                      <p className="text-sm text-destructive">
                        {errors.demands[i]?.categoryId?.message}
                      </p>
                    )}
                  </div>
                  <div className="flex flex-col gap-1">
                    <div className="flex items-center gap-1">
                      <Input
                        aria-label="Quantity in kilograms"
                        inputMode="decimal"
                        className="w-24"
                        placeholder="0.00"
                        {...register(`demands.${i}.quantityKg`)}
                      />
                      <span className="text-sm text-muted-foreground">kg</span>
                    </div>
                    {errors.demands?.[i]?.quantityKg && (
                      <p className="text-sm text-destructive">
                        {errors.demands[i]?.quantityKg?.message}
                      </p>
                    )}
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    aria-label="Remove demand row"
                    disabled={fields.length <= 1}
                    onClick={() => remove(i)}
                  >
                    <Trash2 />
                  </Button>
                </div>
              ))}
              {demandsError && <p className="text-sm text-destructive">{demandsError}</p>}
              <div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => append({ categoryId: '', quantityKg: '' })}
                >
                  <Plus />
                  Add demand row
                </Button>
              </div>
            </fieldset>
          )}

          <div className="flex gap-2">
            <Button type="submit" disabled={pending}>
              Save
            </Button>
            <Button type="button" variant="outline" onClick={() => onDone(null)}>
              Cancel
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
