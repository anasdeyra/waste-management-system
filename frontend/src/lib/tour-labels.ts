import type { TourStatus } from '@/types/api'

/** Display labels of the tour lifecycle states. */
export const STATUS_LABELS: Record<TourStatus, string> = {
  DRAFT: 'Draft',
  PLANNED: 'Planned',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
}
