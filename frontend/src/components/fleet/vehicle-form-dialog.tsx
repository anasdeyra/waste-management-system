import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useCreateVehicle, useUpdateVehicle } from '@/hooks/use-vehicles'
import { ApiRequestError } from '@/lib/api-client'
import { isValidKg } from '@/lib/validation'
import type { Vehicle } from '@/types/api'

const schema = z.object({
  registration: z.string().trim().min(1, 'Registration is required').max(20, 'At most 20 characters'),
  capacityKg: z
    .string()
    .refine(isValidKg, 'Enter a positive number of kilograms (at most 2 decimals)'),
})
type FormValues = z.infer<typeof schema>

/** Create or edit a vehicle (registration and kilogram capacity). */
export function VehicleFormDialog({
  open,
  vehicle,
  onOpenChange,
}: {
  open: boolean
  vehicle?: Vehicle
  onOpenChange: (open: boolean) => void
}) {
  const create = useCreateVehicle()
  const update = useUpdateVehicle()
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { registration: '', capacityKg: '' },
  })

  useEffect(() => {
    if (open) {
      reset({
        registration: vehicle?.registration ?? '',
        capacityKg: vehicle ? String(vehicle.capacityKg) : '',
      })
    }
  }, [open, vehicle, reset])

  const pending = create.isPending || update.isPending

  const onError = (err: Error) => {
    if (err instanceof ApiRequestError && err.status === 409) {
      setError('registration', { message: err.message })
    } else if (err instanceof ApiRequestError && err.status === 400 && err.body?.fieldErrors) {
      for (const fe of err.body.fieldErrors) {
        setError(fe.field as keyof FormValues, { message: fe.message })
      }
    } else {
      toast.error(err.message)
    }
  }

  const onSubmit = (v: FormValues) => {
    const body = { registration: v.registration, capacityKg: Number(v.capacityKg) }
    const done = () => {
      toast.success(vehicle ? 'Vehicle updated' : 'Vehicle created')
      onOpenChange(false)
    }
    if (vehicle) update.mutate({ id: vehicle.id, body }, { onSuccess: done, onError })
    else create.mutate(body, { onSuccess: done, onError })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{vehicle ? 'Edit vehicle' : 'New vehicle'}</DialogTitle>
          <DialogDescription>
            Registrations are stored in upper case and must be unique. Capacity is in kilograms.
          </DialogDescription>
        </DialogHeader>
        <form
          id="vehicle-form"
          onSubmit={handleSubmit(onSubmit)}
          className="flex flex-col gap-4"
          noValidate
        >
          <div className="flex flex-col gap-2">
            <Label htmlFor="vehicle-registration">Registration</Label>
            <Input id="vehicle-registration" autoComplete="off" {...register('registration')} />
            {errors.registration && (
              <p className="text-sm text-destructive">{errors.registration.message}</p>
            )}
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="vehicle-capacity">Capacity (kg)</Label>
            <Input
              id="vehicle-capacity"
              inputMode="decimal"
              autoComplete="off"
              {...register('capacityKg')}
            />
            {errors.capacityKg && (
              <p className="text-sm text-destructive">{errors.capacityKg.message}</p>
            )}
          </div>
        </form>
        <DialogFooter>
          <Button type="submit" form="vehicle-form" disabled={pending}>
            {vehicle ? 'Save changes' : 'Create vehicle'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
