import { Progress } from '@/components/ui/progress'
import { cn } from '@/lib/utils'
import type { TourCapacity } from '@/types/api'

/** Planned demand against the vehicle capacity (one shared capacity, D-024). */
export function CapacityIndicator({ capacity }: { capacity: TourCapacity }) {
  const total = capacity.totalPlannedDemandKg
  const cap = capacity.vehicleCapacityKg
  const percent = cap && cap > 0 ? Math.min(100, (total / cap) * 100) : 0
  return (
    <div className="flex flex-col gap-1">
      <p className="text-sm font-medium">
        {cap === undefined
          ? `Planned demand ${total} kg — no vehicle assigned`
          : `Planned demand ${total} kg of ${cap} kg`}
      </p>
      <Progress
        value={percent}
        aria-label="Planned demand against vehicle capacity"
        className={cn('h-2', capacity.overCapacity && 'bg-red-200 [&>div]:bg-red-600')}
      />
      {capacity.overCapacity && capacity.remainingKg !== undefined && (
        <p className="text-sm font-medium text-red-600">
          Over capacity by {Math.abs(capacity.remainingKg)} kg
        </p>
      )}
      <p className="text-xs text-muted-foreground">
        Mixed categories share one vehicle capacity (no compartments).
      </p>
    </div>
  )
}
