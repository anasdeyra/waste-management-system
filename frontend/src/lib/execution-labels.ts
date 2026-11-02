import type { TourStopStatus } from '@/types/api'

export const STOP_STATUS_LABELS: Record<TourStopStatus, string> = {
  PENDING: 'Pending',
  COLLECTED: 'Collected',
  NOT_COLLECTED: 'Not collected',
}

/** Marker colours of the execution map (also used by the planner map for non-draft tours). */
export const STOP_STATUS_COLORS: Record<TourStopStatus, string> = {
  PENDING: '#2563eb',
  COLLECTED: '#16a34a',
  NOT_COLLECTED: '#dc2626',
}

/** Crew role of the signed-in agent: the list and detail only contain the caller's tours. */
export function crewRole(
  driverId: string | undefined,
  agentId: string | undefined,
): 'Driver' | 'Collector' {
  return driverId !== undefined && agentId !== undefined && driverId === agentId
    ? 'Driver'
    : 'Collector'
}
