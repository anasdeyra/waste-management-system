import type { DataOrigin } from '@/types/api'

/** WGS84 position with explicit field names (never a bare coordinate pair). */
export type LonLat = { longitude: number; latitude: number }

/** Centre of the MN01 case study (Lower Manhattan). */
export const MN01_CENTER: LonLat = { longitude: -74.009, latitude: 40.7165 }

/** Default camera: MN01 centre at street-block zoom. */
export const DEFAULT_ZOOM = 15

/**
 * Panning limit: Manhattan with a margin of river on each side. The map cannot be dragged or
 * zoomed out beyond it (MIN_ZOOM keeps the whole island in view on a typical map panel).
 */
export const MANHATTAN_BOUNDS: { southWest: LonLat; northEast: LonLat } = {
  southWest: { longitude: -74.06, latitude: 40.68 },
  northEast: { longitude: -73.89, latitude: 40.89 },
}
export const MIN_ZOOM = 12

/**
 * Leaflet takes [latitude, longitude]. This helper is the only place the order flips; never
 * write [p.longitude, p.latitude] for Leaflet.
 */
export function toLatLng(p: LonLat): [number, number] {
  return [p.latitude, p.longitude]
}

/** GeoJSON LineString coordinates ([longitude, latitude]) as Leaflet positions. */
export function lineToLatLngs(coordinates: [number, number][]): [number, number][] {
  return coordinates.map(([longitude, latitude]) => toLatLng({ longitude, latitude }))
}

const ORIGIN_LABELS: Record<DataOrigin, string> = {
  IMPORTED_REAL: 'Real (imported)',
  SYNTHETIC: 'Synthetic',
  USER_ENTERED: 'User-entered',
}

/** Human-readable provenance of a value (NFR-12). */
export function originLabel(o: DataOrigin): string {
  return ORIGIN_LABELS[o]
}

/** Coordinates as labelled text, six decimals, longitude first. */
export function formatCoordinates(p: LonLat): string {
  return `longitude ${p.longitude.toFixed(6)}, latitude ${p.latitude.toFixed(6)}`
}
