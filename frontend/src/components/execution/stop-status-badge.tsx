import { Badge } from '@/components/ui/badge'
import { STOP_STATUS_LABELS } from '@/lib/execution-labels'
import type { TourStopStatus } from '@/types/api'

const VARIANTS = {
  PENDING: 'secondary',
  COLLECTED: 'default',
  NOT_COLLECTED: 'destructive',
} as const

/** Outcome state of one stop. */
export function StopStatusBadge({ status }: { status: TourStopStatus }) {
  return <Badge variant={VARIANTS[status]}>{STOP_STATUS_LABELS[status]}</Badge>
}
