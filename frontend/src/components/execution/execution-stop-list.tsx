import { useState } from 'react'

import { StopOutcomeForm } from '@/components/execution/stop-outcome-form'
import { StopOutcomeSummary } from '@/components/execution/stop-outcome-summary'
import { StopStatusBadge } from '@/components/execution/stop-status-badge'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import type { Tour, TourStop } from '@/types/api'

/** Accessible ordered stop list of an assigned tour: the map-free alternative (NFR-03). */
export function ExecutionStopList({
  tour,
  selectedStopId,
  onSelect,
}: {
  tour: Tour
  selectedStopId: string | null
  onSelect: (stop: TourStop) => void
}) {
  const [open, setOpen] = useState<string | null>(null)
  return (
    <ol className="flex flex-col gap-2">
      {tour.stops.map((s) => (
        <li
          key={s.id}
          className={`rounded-md border p-2 ${s.id === selectedStopId ? 'border-primary' : ''}`}
        >
          <div className="flex items-start gap-2">
            <Badge variant="secondary" className="mt-0.5">
              {s.visitOrder}
            </Badge>
            <div className="min-w-0 flex-1 text-sm">
              <span className="font-medium">{s.name}</span>
              {s.sourceIdentifier && (
                <span className="ml-2 text-xs text-muted-foreground">{s.sourceIdentifier}</span>
              )}
              <div className="text-xs text-muted-foreground">
                {s.plannedDemands.map((d) => `${d.categoryName} ${d.plannedQuantityKg} kg`).join(' · ')}
                {' — planned total '}
                {s.plannedTotalKg} kg
              </div>
              <StopOutcomeSummary stop={s} />
            </div>
            <StopStatusBadge status={s.status} />
            <Button variant="outline" size="sm" onClick={() => onSelect(s)}>
              Show on map
            </Button>
            {tour.status === 'IN_PROGRESS' && s.status === 'PENDING' && (
              <Button
                variant="outline"
                size="sm"
                aria-expanded={open === s.id}
                onClick={() => setOpen(open === s.id ? null : s.id)}
              >
                Record outcome
              </Button>
            )}
          </div>
          {open === s.id && tour.status === 'IN_PROGRESS' && s.status === 'PENDING' && (
            <StopOutcomeForm tour={tour} stop={s} />
          )}
        </li>
      ))}
    </ol>
  )
}
