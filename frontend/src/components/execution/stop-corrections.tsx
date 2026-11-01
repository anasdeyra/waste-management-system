import { STOP_STATUS_LABELS } from '@/lib/execution-labels'
import type { CorrectionQuantity, TourStop } from '@/types/api'

function quantities(list: CorrectionQuantity[]): string {
  return list.map((q) => `${q.categoryName} ${q.quantityKg} kg`).join(' · ')
}

/** Retained corrections of a stop, oldest first; nothing when the stop was never corrected. */
export function StopCorrections({ stop }: { stop: TourStop }) {
  if (stop.corrections.length === 0) return null
  return (
    <div className="mt-1 rounded-md bg-muted/50 p-2 text-xs">
      <div className="font-medium">Corrections</div>
      <ul className="flex flex-col gap-1">
        {stop.corrections.map((c) => (
          <li key={c.id}>
            <div>
              {c.correctedBy} at {new Date(c.correctedAt).toLocaleString()}:{' '}
              {STOP_STATUS_LABELS[c.previousStatus]} → {STOP_STATUS_LABELS[c.newStatus]}
            </div>
            <div className="text-muted-foreground">
              Before: {quantities(c.previousQuantities) || c.previousIncidentReason || '—'}
              {' · '}
              After: {quantities(c.newQuantities) || c.newIncidentReason || '—'}
            </div>
            <div>Reason: {c.reason}</div>
          </li>
        ))}
      </ul>
    </div>
  )
}
