import { useState } from 'react'
import { toast } from 'sonner'

import { Alert, AlertDescription } from '@/components/ui/alert'
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
import { Textarea } from '@/components/ui/textarea'
import { useAllCategories } from '@/hooks/use-collection-points'
import { useCorrectStopOutcome } from '@/hooks/use-tours'
import { isValidQuantityKg } from '@/lib/validation'
import type { Tour, TourStop } from '@/types/api'

interface Row {
  categoryId: string
  name: string
  value: string
}

function initialRows(stop: TourStop): Row[] {
  if (stop.actualQuantities.length > 0) {
    return stop.actualQuantities.map((q) => ({
      categoryId: q.categoryId,
      name: q.categoryName,
      value: String(q.actualQuantityKg),
    }))
  }
  return stop.plannedDemands.map((d) => ({
    categoryId: d.categoryId,
    name: d.categoryName,
    value: String(d.plannedQuantityKg),
  }))
}

function CorrectionForm({ tour, stop, onDone }: { tour: Tour; stop: TourStop; onDone: () => void }) {
  const categories = useAllCategories()
  const correct = useCorrectStopOutcome()
  const [collected, setCollected] = useState(stop.status !== 'NOT_COLLECTED')
  const [rows, setRows] = useState<Row[]>(() => initialRows(stop))
  const [incident, setIncident] = useState(stop.incidentReason ?? '')
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)

  const listed = new Set(rows.map((r) => r.categoryId))
  const addable = (categories.data ?? []).filter((c) => c.active && !listed.has(c.id))
  const idPrefix = `correct-${stop.id}`

  const submit = () => {
    setError(null)
    if (collected) {
      if (rows.length === 0) return setError('A collected stop needs at least one quantity.')
      if (rows.some((r) => !isValidQuantityKg(r.value))) {
        return setError('Quantities must be zero or positive with at most two decimals.')
      }
    } else if (incident.trim() === '') {
      return setError('A not-collected stop needs a reason.')
    }
    if (reason.trim() === '') return setError('A correction needs a reason.')
    const body = collected
      ? {
          status: 'COLLECTED' as const,
          quantities: rows.map((r) => ({
            categoryId: r.categoryId,
            actualQuantityKg: Number(r.value),
          })),
          incidentReason: incident.trim() || undefined,
          reason: reason.trim(),
        }
      : { status: 'NOT_COLLECTED' as const, incidentReason: incident.trim(), reason: reason.trim() }
    correct.mutate(
      { id: tour.id, stopId: stop.id, body },
      {
        onSuccess: () => {
          toast.success(`Outcome of stop ${stop.visitOrder} corrected.`)
          onDone()
        },
        onError: (e) => setError(e.message),
      },
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <fieldset className="flex flex-wrap gap-4">
        <legend className="sr-only">Corrected outcome of stop {stop.visitOrder}</legend>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="radio"
            name={`${idPrefix}-status`}
            checked={collected}
            onChange={() => setCollected(true)}
          />
          Collected
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            type="radio"
            name={`${idPrefix}-status`}
            checked={!collected}
            onChange={() => setCollected(false)}
          />
          Not collected
        </label>
      </fieldset>
      {collected && (
        <div className="flex flex-col gap-2">
          {rows.map((r, i) => (
            <div key={r.categoryId} className="flex items-center gap-2">
              <Label htmlFor={`${idPrefix}-q-${r.categoryId}`} className="w-32 shrink-0">
                {r.name} (kg)
              </Label>
              <Input
                id={`${idPrefix}-q-${r.categoryId}`}
                type="number"
                min={0}
                step={0.01}
                value={r.value}
                onChange={(e) =>
                  setRows((prev) => prev.map((x, j) => (j === i ? { ...x, value: e.target.value } : x)))
                }
                className="w-32"
              />
              <Button
                variant="outline"
                size="sm"
                onClick={() => setRows((prev) => prev.filter((_, j) => j !== i))}
              >
                Remove
              </Button>
            </div>
          ))}
          {addable.length > 0 && (
            <div className="flex items-center gap-2">
              <Label htmlFor={`${idPrefix}-add`} className="w-32 shrink-0">
                Add another category
              </Label>
              <select
                id={`${idPrefix}-add`}
                className="h-9 rounded-md border bg-background px-2 text-sm"
                value=""
                onChange={(e) => {
                  const c = addable.find((x) => x.id === e.target.value)
                  if (c) setRows((prev) => [...prev, { categoryId: c.id, name: c.name, value: '0' }])
                }}
              >
                <option value="">Select…</option>
                {addable.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
      )}
      <div className="flex flex-col gap-1">
        <Label htmlFor={`${idPrefix}-incident`}>Incident or reason</Label>
        <Textarea
          id={`${idPrefix}-incident`}
          value={incident}
          maxLength={500}
          onChange={(e) => setIncident(e.target.value)}
        />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor={`${idPrefix}-reason`}>Correction reason</Label>
        <Textarea
          id={`${idPrefix}-reason`}
          value={reason}
          maxLength={500}
          onChange={(e) => setReason(e.target.value)}
        />
      </div>
      <p className="text-xs text-muted-foreground">The previous values are kept in the stop history.</p>
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <DialogFooter>
        <Button variant="outline" onClick={onDone}>
          Close
        </Button>
        <Button disabled={correct.isPending} onClick={submit}>
          Save correction
        </Button>
      </DialogFooter>
    </div>
  )
}

/** Planner correction of one recorded stop outcome (US-31); the previous values are retained. */
export function StopCorrectionDialog({ tour, stop }: { tour: Tour; stop: TourStop }) {
  const [open, setOpen] = useState(false)
  return (
    <>
      <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
        Correct
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Correct stop {stop.visitOrder} outcome</DialogTitle>
            <DialogDescription>{stop.name}</DialogDescription>
          </DialogHeader>
          <CorrectionForm tour={tour} stop={stop} onDone={() => setOpen(false)} />
        </DialogContent>
      </Dialog>
    </>
  )
}
