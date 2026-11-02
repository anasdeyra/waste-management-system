import type { ResourceStatus, TourStatus } from '@/types/api'

export function resourceLink(kind: 'vehicles' | 'agents', status: ResourceStatus): string {
  return `/${kind}?status=${status}`
}

export function pointLink(status: 'ACTIVE' | 'INACTIVE'): string {
  return `/collection-points?status=${status}`
}

/** Open tours link to the tour list; finished ones to the history, with the selected period. */
export function tourStatusLink(status: TourStatus, from?: string, to?: string): string {
  if (status === 'COMPLETED' || status === 'CANCELLED') {
    const q = new URLSearchParams({ status })
    if (from) q.set('from', from)
    if (to) q.set('to', to)
    return `/history?${q.toString()}`
  }
  const q = new URLSearchParams({ status })
  if (from && from === to) q.set('planningDate', from)
  return `/tours?${q.toString()}`
}
