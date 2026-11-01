import { ArrowDown, ArrowUp, Trash2 } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { TourStop } from '@/types/api'

/** Ordered stop list: the accessible, map-free editor of stops and their input order. */
export function TourStopList({
  stops,
  selectedStopId,
  editable,
  busy,
  onSelect,
  onReorder,
  onRemove,
}: {
  stops: TourStop[]
  selectedStopId: string | null
  editable: boolean
  busy: boolean
  onSelect: (stop: TourStop) => void
  onReorder: (stopIds: string[]) => void
  onRemove: (stopId: string) => void
}) {
  if (stops.length === 0) {
    return <p className="text-sm text-muted-foreground">No stops yet. Add points from the map or the list.</p>
  }
  const move = (index: number, delta: number) => {
    const ids = stops.map((s) => s.id)
    const [moved] = ids.splice(index, 1)
    ids.splice(index + delta, 0, moved)
    onReorder(ids)
  }
  return (
    <ol className="flex flex-col gap-2">
      {stops.map((s, i) => (
        <li
          key={s.id}
          className={`flex items-start gap-2 rounded-md border p-2 ${s.id === selectedStopId ? 'border-primary' : ''}`}
        >
          <Badge variant="secondary" className="mt-0.5">
            {s.visitOrder}
          </Badge>
          <div className="min-w-0 flex-1 text-sm">
            <button
              type="button"
              className="text-left font-medium underline-offset-2 hover:underline"
              onClick={() => onSelect(s)}
            >
              {s.name}
            </button>
            {s.sourceIdentifier && (
              <span className="ml-2 text-xs text-muted-foreground">{s.sourceIdentifier}</span>
            )}
            {s.pointStatus !== 'ACTIVE' && (
              <Badge variant="destructive" className="ml-2">
                Inactive
              </Badge>
            )}
            {s.pointStatus === 'ACTIVE' && !s.optimizationEligible && (
              <Badge variant="destructive" className="ml-2">
                Not eligible
              </Badge>
            )}
            <div className="text-xs text-muted-foreground">
              {s.plannedDemands.map((d) => `${d.categoryName} ${d.plannedQuantityKg} kg`).join(' · ')}
              {' — total '}
              {s.plannedTotalKg} kg
            </div>
          </div>
          {editable && (
            <div className="flex gap-1">
              <Button
                variant="outline"
                size="icon"
                aria-label="Move up"
                disabled={busy || i === 0}
                onClick={() => move(i, -1)}
              >
                <ArrowUp />
              </Button>
              <Button
                variant="outline"
                size="icon"
                aria-label="Move down"
                disabled={busy || i === stops.length - 1}
                onClick={() => move(i, 1)}
              >
                <ArrowDown />
              </Button>
              <Button
                variant="outline"
                size="icon"
                aria-label="Remove stop"
                disabled={busy}
                onClick={() => onRemove(s.id)}
              >
                <Trash2 />
              </Button>
            </div>
          )}
        </li>
      ))}
    </ol>
  )
}
