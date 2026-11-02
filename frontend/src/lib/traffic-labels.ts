import type { SpeedBand, TrafficLink, TrafficLinkStatus } from '@/types/api'

export const TRAFFIC_STATUS_LABELS: Record<TrafficLinkStatus, string> = {
  OBSERVED: 'Observed',
  STALE: 'Stale reading',
  NO_DATA: 'Sensor link, no valid reading',
}

export const SPEED_BAND_LABELS: Record<SpeedBand, string> = {
  BELOW_10_MPH: 'Under 10 mph',
  FROM_10_TO_25_MPH: '10–25 mph',
  FROM_25_MPH: '25 mph and over',
}

export const SPEED_BAND_COLORS: Record<SpeedBand, string> = {
  BELOW_10_MPH: '#dc2626',
  FROM_10_TO_25_MPH: '#d97706',
  FROM_25_MPH: '#16a34a',
}

export interface TrafficLineStyle {
  color: string
  weight: number
  dashArray?: string
}

export function trafficLineStyle(link: Pick<TrafficLink, 'status' | 'speedBand'>): TrafficLineStyle {
  if (link.status === 'OBSERVED' && link.speedBand) {
    return { color: SPEED_BAND_COLORS[link.speedBand], weight: 5 }
  }
  if (link.status === 'STALE') return { color: '#6b7280', weight: 4, dashArray: '6 6' }
  return { color: '#9ca3af', weight: 3, dashArray: '2 6' }
}

/** Age of a snapshot: under an hour in minutes, under 48 hours in hours, else days. */
export function formatAge(minutes: number): string {
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  if (hours < 48) return `${hours} h`
  return `${Math.floor(hours / 24)} days`
}
