import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useDepots } from '@/hooks/use-collection-points'
import { useCreateTour } from '@/hooks/use-tours'

/** Today's local date as YYYY-MM-DD (the backend decides the final date rule). */
function todayLocal(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const schema = z.object({
  planningDate: z
    .string()
    .min(1, 'Planning date is required')
    .refine((v) => v >= todayLocal(), 'The planning date must be today or later'),
  depotId: z.string().min(1, 'Choose a depot'),
})
type FormValues = z.infer<typeof schema>

/** Creates a draft tour (planning date + depot) and opens its planner. */
export function TourCreateDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const navigate = useNavigate()
  const depots = useDepots()
  const create = useCreateTour()
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    control,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { planningDate: todayLocal(), depotId: '' },
  })
  const depotId = useWatch({ control, name: 'depotId' })

  useEffect(() => {
    if (open) reset({ planningDate: todayLocal(), depotId: depots.data?.[0]?.id ?? '' })
  }, [open, depots.data, reset])

  const onSubmit = (v: FormValues) =>
    create.mutate(v, {
      onSuccess: (tour) => {
        toast.success('Draft tour created')
        onOpenChange(false)
        navigate(`/tours/${tour.id}`)
      },
      onError: (err) => toast.error(err.message),
    })

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New draft tour</DialogTitle>
          <DialogDescription>
            A draft may stay incomplete; stops, vehicle and crew are added in the planner.
          </DialogDescription>
        </DialogHeader>
        <form
          id="tour-create-form"
          onSubmit={handleSubmit(onSubmit)}
          className="flex flex-col gap-4"
          noValidate
        >
          <div className="flex flex-col gap-2">
            <Label htmlFor="tour-date">Planning date</Label>
            <Input id="tour-date" type="date" {...register('planningDate')} />
            {errors.planningDate && (
              <p className="text-sm text-destructive">{errors.planningDate.message}</p>
            )}
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="tour-depot">Depot</Label>
            <Select value={depotId} onValueChange={(v) => setValue('depotId', v, { shouldValidate: true })}>
              <SelectTrigger id="tour-depot">
                <SelectValue placeholder="Choose a depot" />
              </SelectTrigger>
              <SelectContent>
                {(depots.data ?? []).map((d) => (
                  <SelectItem key={d.id} value={d.id}>
                    {d.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.depotId && <p className="text-sm text-destructive">{errors.depotId.message}</p>}
          </div>
        </form>
        <DialogFooter>
          <Button type="submit" form="tour-create-form" disabled={create.isPending}>
            Create draft
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
