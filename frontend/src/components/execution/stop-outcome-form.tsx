import { useState } from 'react'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useAllCategories } from '@/hooks/use-collection-points'
import { useRecordStopOutcome } from '@/hooks/use-my-tours'
import type { Tour, TourStop } from '@/types/api'

interface Row {
  categoryId: string
  name: string
  value: string
}

const TWO_DECIMALS = /^\d{1,8}(\.\d{1,2})?$/

/** Records the outcome of one pending stop of an in-progress tour (US-28). */
export function StopOutcomeForm({ tour, stop }: { tour: Tour; stop: TourStop }) {
  const categories = useAllCategories()
  const record = useRecordStopOutcome()
  const [collected, setCollected] = useState(true)
  const [rows, setRows] = useState<Row[]>(() =>
    stop.plannedDemands.map((d) => ({
      categoryId: d.categoryId,
      name: d.categoryName,
      value: String(d.plannedQuantityKg),
    })),
  )
  const [incidentNote, setIncidentNote] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState<string | null>(null)

  const listed = new Set(rows.map((r) => r.categoryId))
  const addable = (categories.data ?? []).filter((c) => c.active && !listed.has(c.id))
  const idPrefix = `outcome-${stop.id}`

  const submit = () => {
    setError(null)
    if (collected) {
      if (rows.length === 0) return setError('A collected stop needs at least one quantity.')
      if (rows.some((r) => !TWO_DECIMALS.test(r.value.trim()))) {
        return setError('Quantities must be zero or positive with at most two decimals.')
      }
    } else if (reason.trim() === '') {
      return setError('A not-collected stop needs a reason.')
    }
    const body = collected
      ? {
          status: 'COLLECTED' as const,
          quantities: rows.map((r) => ({
            categoryId: r.categoryId,
            actualQuantityKg: Number(r.value),
          })),
          incidentReason: incidentNote.trim() || undefined,
        }
      : { status: 'NOT_COLLECTED' as const, incidentReason: reason.trim() }
    record.mutate(
      { id: tour.id, stopId: stop.id, body },
      {
        onSuccess: () => toast.success(`Outcome recorded for stop ${stop.visitOrder}.`),
        onError: (e) => toast.error(e.message),
      },
    )
  }

  return (
    <div className="mt-2 flex flex-col gap-3 rounded-md border bg-muted/30 p-3">
      <fieldset className="flex flex-wrap gap-4">
        <legend className="sr-only">Outcome of stop {stop.visitOrder}</legend>
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
      {collected ? (
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
          <div className="flex items-center gap-2">
            <Label htmlFor={`${idPrefix}-note`} className="w-32 shrink-0">
              Incident note (optional)
            </Label>
            <Input
              id={`${idPrefix}-note`}
              value={incidentNote}
              maxLength={500}
              onChange={(e) => setIncidentNote(e.target.value)}
            />
          </div>
        </div>
      ) : (
        <div className="flex flex-col gap-1">
          <Label htmlFor={`${idPrefix}-reason`}>Incident or reason</Label>
          <Textarea
            id={`${idPrefix}-reason`}
            value={reason}
            maxLength={500}
            onChange={(e) => setReason(e.target.value)}
          />
        </div>
      )}
      {error && (
        <p role="alert" className="text-sm text-destructive">
          {error}
        </p>
      )}
      <div>
        <Button size="sm" disabled={record.isPending} onClick={submit}>
          Record outcome
        </Button>
      </div>
    </div>
  )
}
