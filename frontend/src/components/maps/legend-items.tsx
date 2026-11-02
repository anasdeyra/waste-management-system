import type { ReactNode } from 'react'

import { METHOD_LABELS, METHOD_STYLES } from '@/lib/evaluation-labels'
import { STOP_STATUS_COLORS, STOP_STATUS_LABELS } from '@/lib/execution-labels'
import type { PlanningMethod } from '@/types/api'

import { MARKER } from './map-theme'

export type LegendItem = { label: string; swatch: ReactNode }

/** Swatches that match the marker shapes in map-theme.ts. */
export const Swatch = {
  dot: (fill: string, ring: string = MARKER.ring, dashed = false) => (
    <span
      className="inline-block size-3 shrink-0 rounded-full shadow-[0_0_0_1px_rgb(0_0_0/0.15)]"
      style={{ backgroundColor: fill, border: `2px ${dashed ? 'dashed' : 'solid'} ${ring}` }}
    />
  ),
  badge: (fill: string, dashedRing?: string) => (
    <span
      className="inline-flex size-4 shrink-0 items-center justify-center rounded-full text-[9px] font-bold text-white"
      style={{
        backgroundColor: fill,
        boxShadow: dashedRing ? undefined : '0 0 0 2px #fff, 0 1px 2px rgb(0 0 0 / 0.3)',
        outline: dashedRing ? `2px dashed ${dashedRing}` : undefined,
        outlineOffset: 1,
      }}
    >
      1
    </span>
  ),
  depot: () => (
    <span
      className="inline-block size-3.5 shrink-0 rounded-[4px] shadow-[0_0_0_2px_#fff,0_1px_2px_rgb(0_0_0/0.3)]"
      style={{ backgroundColor: MARKER.depot }}
    />
  ),
  line: (color: string, dashed = false) => (
    <span
      className="inline-block h-0 w-5 shrink-0"
      style={{ borderTop: `3px ${dashed ? 'dashed' : 'solid'} ${color}` }}
    />
  ),
}

/** Collection points map: depot, active, inactive, selected. */
export const POINTS_LEGEND: LegendItem[] = [
  { label: 'Depot', swatch: Swatch.depot() },
  { label: 'Active point', swatch: Swatch.dot(MARKER.point) },
  { label: 'Inactive point', swatch: Swatch.dot(MARKER.ring, MARKER.pointInactive, true) },
  { label: 'Selected', swatch: Swatch.dot(MARKER.point, MARKER.ink) },
]

const EXECUTION_ITEMS: LegendItem[] = (['PENDING', 'COLLECTED', 'NOT_COLLECTED'] as const).map((s) => ({
  label: STOP_STATUS_LABELS[s],
  swatch: Swatch.badge(STOP_STATUS_COLORS[s]),
}))

function routeItem(method: PlanningMethod | undefined): LegendItem[] {
  if (!method) return []
  const color = method === 'NN_PLUS_2_OPT' ? '#16a34a' : METHOD_STYLES[method].color
  return [{ label: `Road route (${METHOD_LABELS[method]})`, swatch: Swatch.line(color) }]
}

/** Tour planner map: planned or executed stops, candidates, input-order line and route. */
export function plannerLegend(
  draft: boolean,
  editable: boolean,
  flagged: boolean,
  routeMethod?: PlanningMethod,
): LegendItem[] {
  return [
    { label: 'Depot', swatch: Swatch.depot() },
    ...(draft ? [{ label: 'Stop (visit order)', swatch: Swatch.badge(MARKER.ink) }] : EXECUTION_ITEMS),
    ...(editable ? [{ label: 'Candidate', swatch: Swatch.dot(MARKER.candidate) }] : []),
    ...(flagged ? [{ label: 'Needs attention', swatch: Swatch.badge(MARKER.ink, MARKER.flagged) }] : []),
    { label: 'Input order', swatch: Swatch.line(MARKER.inputOrder, true) },
    ...routeItem(routeMethod),
  ]
}

/** Crew execution map: outcome colours and the attached route. */
export function executionLegend(routeMethod?: PlanningMethod): LegendItem[] {
  return [{ label: 'Depot', swatch: Swatch.depot() }, ...EXECUTION_ITEMS, ...routeItem(routeMethod)]
}

/** Evaluation comparison map (method lines are listed under the map). */
export const EVALUATION_LEGEND: LegendItem[] = [
  { label: 'Depot', swatch: Swatch.depot() },
  { label: 'Stop (numbered by the chosen method)', swatch: Swatch.badge(MARKER.ink) },
]
