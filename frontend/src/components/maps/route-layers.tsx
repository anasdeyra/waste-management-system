import L from 'leaflet'
import { useEffect, useMemo } from 'react'
import { CircleMarker, Polyline, Popup, Tooltip, useMap } from 'react-leaflet'

import { METHOD_LABELS, METHOD_ORDER, METHOD_STYLES } from '@/lib/evaluation-labels'
import type { EvaluationRun, PlanningMethod, RoadLegCollection, TourRoute } from '@/types/api'

import { lineToLatLngs, toLatLng } from './geo'

type LineStyle = { color: string; weight: number; opacity: number }

/** One road-following route: every leg is a line along the road graph, not a straight segment. */
export function RoadRouteLine({
  legs,
  style,
  label,
}: {
  legs: RoadLegCollection
  style: LineStyle
  label: string
}) {
  return (
    <Polyline
      positions={legs.features.map((f) => lineToLatLngs(f.geometry.coordinates))}
      pathOptions={{ color: style.color, weight: style.weight, opacity: style.opacity }}
    >
      <Tooltip sticky>{label}</Tooltip>
    </Polyline>
  )
}

function FitToRun({ run }: { run: EvaluationRun }) {
  const map = useMap()
  const positions = useMemo(
    () => [toLatLng(run.depot), ...run.stops.map((s) => toLatLng(s))],
    // The frozen run never changes for a given id.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [run.id],
  )
  useEffect(() => {
    map.fitBounds(L.latLngBounds(positions), { padding: [24, 24] })
  }, [map, positions])
  return null
}

/**
 * Comparison layers of one retained run: the depot, one toggleable road route per feasible
 * method and the frozen stops numbered by the chosen method. Coordinates come from the run,
 * not from the live tour, so the map shows exactly what was evaluated.
 */
export function EvaluationRouteLayers({
  run,
  visible,
  numbersFor,
}: {
  run: EvaluationRun
  visible: Record<PlanningMethod, boolean>
  numbersFor: PlanningMethod
}) {
  const numbering = run.results.find((r) => r.method === numbersFor && r.feasible)
  const feasible = run.results.filter((r) => r.feasible)
  return (
    <>
      <FitToRun run={run} />
      {METHOD_ORDER.map((method) => {
        const result = run.results.find((r) => r.method === method)
        if (!result || !result.feasible || !result.legs || !visible[method]) return null
        return (
          <RoadRouteLine
            key={method}
            legs={result.legs}
            style={METHOD_STYLES[method]}
            label={METHOD_LABELS[method]}
          />
        )
      })}
      <CircleMarker
        center={toLatLng(run.depot)}
        radius={10}
        pathOptions={{ color: '#7c3aed', fillColor: '#7c3aed', fillOpacity: 0.9 }}
      >
        <Tooltip>Depot: {run.depot.name}</Tooltip>
      </CircleMarker>
      {run.stops.map((stop) => {
        const position = numbering ? numbering.visitOrder.indexOf(stop.inputIndex) + 1 : stop.inputIndex
        return (
          <CircleMarker
            key={stop.collectionPointId}
            center={toLatLng(stop)}
            radius={9}
            pathOptions={{ color: '#1d4ed8', fillColor: '#3b82f6', fillOpacity: 0.9, weight: 2 }}
          >
            <Tooltip permanent direction="center" className="tour-stop-number">
              {position}
            </Tooltip>
            <Popup>
              <div className="space-y-1 text-sm">
                <div className="font-semibold">{stop.name}</div>
                <ul>
                  {feasible.map((r) => (
                    <li key={r.method}>
                      {METHOD_LABELS[r.method]}: visit {r.visitOrder.indexOf(stop.inputIndex) + 1}
                    </li>
                  ))}
                </ul>
              </div>
            </Popup>
          </CircleMarker>
        )
      })}
    </>
  )
}

/** The road route attached to the tour (accepted proposal or kept input order). */
export function TourRouteLayer({ route }: { route: TourRoute }) {
  const style =
    route.method === 'NN_PLUS_2_OPT'
      ? { color: '#16a34a', weight: 5, opacity: 0.9 }
      : METHOD_STYLES[route.method]
  return (
    <RoadRouteLine
      legs={route.legs}
      style={style}
      label={`Road route: ${METHOD_LABELS[route.method]}`}
    />
  )
}
