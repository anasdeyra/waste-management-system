import { Plus } from 'lucide-react'
import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import type { CollectionPoint } from '@/types/api'

const MAX_SHOWN = 50

/** Eligible points not yet in the tour, filtered by name or source identifier. */
export function TourCandidateList({
  candidates,
  inTour,
  editable,
  busy,
  onAdd,
}: {
  candidates: CollectionPoint[]
  inTour: Set<string>
  editable: boolean
  busy: boolean
  onAdd: (pointId: string) => void
}) {
  const [filter, setFilter] = useState('')
  const needle = filter.trim().toLowerCase()
  const matches = candidates.filter(
    (p) =>
      !inTour.has(p.id) &&
      (needle === '' ||
        p.name.toLowerCase().includes(needle) ||
        (p.sourceIdentifier ?? '').toLowerCase().includes(needle)),
  )
  const shown = matches.slice(0, MAX_SHOWN)
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor="candidate-filter">Find a collection point</Label>
      <Input
        id="candidate-filter"
        placeholder="Name or source identifier"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
      />
      <ul className="flex max-h-72 flex-col gap-1 overflow-y-auto">
        {shown.map((p) => (
          <li key={p.id} className="flex items-center gap-2 rounded-md border p-2 text-sm">
            <div className="min-w-0 flex-1">
              <div className="truncate font-medium">{p.name}</div>
              <div className="text-xs text-muted-foreground">
                {p.sourceIdentifier ? `${p.sourceIdentifier} · ` : ''}
                {p.totalDemandKg} kg
              </div>
            </div>
            <Button
              variant="outline"
              size="sm"
              disabled={!editable || busy}
              aria-label={`Add ${p.name}`}
              onClick={() => onAdd(p.id)}
            >
              <Plus />
              Add
            </Button>
          </li>
        ))}
        {shown.length === 0 && (
          <li className="text-sm text-muted-foreground">No matching points.</li>
        )}
      </ul>
      {matches.length > MAX_SHOWN && (
        <p className="text-xs text-muted-foreground">Refine the filter to see more</p>
      )}
    </div>
  )
}
