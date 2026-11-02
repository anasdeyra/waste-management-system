import L from 'leaflet'

import { STOP_STATUS_COLORS } from '@/lib/execution-labels'
import type { TourStopStatus } from '@/types/api'

/**
 * Marker palette shared by every map. It sits on the minimal basemap (basemap-style.ts): deep,
 * saturated fills with a white ring so markers separate from parks, water and buildings.
 */
export const MARKER = {
  ink: '#1f2937',
  ring: '#ffffff',
  point: '#0f766e',
  pointInactive: '#94a3b8',
  candidate: '#64748b',
  selected: '#f59e0b',
  flagged: '#dc2626',
  picked: '#ea580c',
  depot: '#4c1d95',
  inputOrder: '#475569',
} as const

/** Dot radius that shrinks as the map zooms out, so dense areas stay readable. */
export function pointRadius(zoom: number, selected = false): number {
  const base = zoom >= 17 ? 6.5 : zoom >= 16 ? 5.5 : zoom >= 15 ? 4.5 : zoom >= 14 ? 3.5 : 2.5
  return selected ? base + 3 : base
}

/** Vector style of a collection point dot (FR-22). */
export function pointPathOptions(active: boolean, selected: boolean): L.PathOptions {
  return {
    className: 'wm-point',
    color: selected ? MARKER.ink : MARKER.ring,
    weight: selected ? 3 : 1.5,
    fillColor: active ? MARKER.point : MARKER.ring,
    fillOpacity: 1,
    ...(active ? {} : { color: selected ? MARKER.ink : MARKER.pointInactive, dashArray: '2 2' }),
  }
}

const iconCache = new Map<string, L.DivIcon>()

function cached(key: string, make: () => L.DivIcon): L.DivIcon {
  let icon = iconCache.get(key)
  if (!icon) {
    icon = make()
    iconCache.set(key, icon)
  }
  return icon
}

// Lucide "warehouse" glyph, inlined so the marker needs no React render.
const WAREHOUSE_SVG =
  '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.25" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M18 21V10a1 1 0 0 0-1-1H7a1 1 0 0 0-1 1v11"/><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8.35a2 2 0 0 1 1.26-1.86l8-3.2a2 2 0 0 1 1.48 0l8 3.2A2 2 0 0 1 22 8.35z"/><path d="M6 13h12"/><path d="M6 17h12"/></svg>'

/** Depot: a dark rounded square with a warehouse glyph, distinct from every round marker. */
export function depotIcon(): L.DivIcon {
  return cached('depot', () =>
    L.divIcon({
      className: 'wm-marker',
      html: `<div class="wm-depot" style="background:${MARKER.depot}">${WAREHOUSE_SVG}</div>`,
      iconSize: [28, 28],
      iconAnchor: [14, 14],
      tooltipAnchor: [0, -16],
      popupAnchor: [0, -16],
    }),
  )
}

export type StopTone = 'planned' | TourStopStatus

export function stopToneColor(tone: StopTone): string {
  return tone === 'planned' ? MARKER.ink : STOP_STATUS_COLORS[tone]
}

/** Numbered stop badge; flagged stops get a red dashed ring, the selected one an amber halo. */
export function stopIcon(number: number, tone: StopTone, selected = false, flagged = false): L.DivIcon {
  const key = `stop:${number}:${tone}:${selected}:${flagged}`
  return cached(key, () => {
    const classes = ['wm-stop', selected && 'is-selected', flagged && 'is-flagged'].filter(Boolean).join(' ')
    return L.divIcon({
      className: 'wm-marker',
      html: `<div class="${classes}" style="background:${stopToneColor(tone)}">${number}</div>`,
      iconSize: [26, 26],
      iconAnchor: [13, 13],
      tooltipAnchor: [0, -15],
      popupAnchor: [0, -15],
    })
  })
}

/** A location picked on the map but not saved yet. */
export function pickedIcon(): L.DivIcon {
  return cached('picked', () =>
    L.divIcon({
      className: 'wm-marker',
      html: `<div class="wm-picked" style="--wm-picked:${MARKER.picked}"></div>`,
      iconSize: [22, 22],
      iconAnchor: [11, 11],
      tooltipAnchor: [0, -14],
    }),
  )
}

/** A white casing under a single route so it reads over white roads. */
export const ROUTE_CASING = { color: '#ffffff', opacity: 0.95 } as const
