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

export const STOP_STATUS_LEGEND = 'Blue: pending · Green: collected · Red: not collected'

/** Crew role of the signed-in agent: the list and detail only contain the caller's tours. */
export function crewRole(
  driverId: string | undefined,
  agentId: string | undefined,
): 'Driver' | 'Collector' {
  return driverId !== undefined && agentId !== undefined && driverId === agentId
    ? 'Driver'
    : 'Collector'
}
