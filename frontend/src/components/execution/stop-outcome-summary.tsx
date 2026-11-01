import type { TourStop } from '@/types/api'

/** One line with the recorded outcome of a terminal stop; nothing for a pending stop. */
export function StopOutcomeSummary({ stop }: { stop: TourStop }) {
  if (stop.status === 'PENDING') return null
  const parts: string[] = []
  if (stop.actualQuantities.length > 0) {
    parts.push(stop.actualQuantities.map((q) => `${q.categoryName} ${q.actualQuantityKg} kg`).join(' · '))
    parts.push(`total ${stop.actualTotalKg ?? 0} kg`)
  }
  if (stop.incidentReason) parts.push(stop.incidentReason)
  const by = stop.outcomeRecordedBy
    ? `by ${stop.outcomeRecordedBy}${stop.outcomeRecordedAt ? ` at ${new Date(stop.outcomeRecordedAt).toLocaleString()}` : ''}`
    : ''
  return (
    <div className="text-xs">
      {parts.join(' — ')}
      {by && <span className="ml-1 text-muted-foreground">{by}</span>}
    </div>
  )
}
