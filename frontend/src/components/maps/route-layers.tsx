import L from 'leaflet'
import { useEffect, useMemo } from 'react'
import { Marker, Polyline, Popup, Tooltip, useMap } from 'react-leaflet'

import { METHOD_LABELS, METHOD_ORDER, METHOD_STYLES } from '@/lib/evaluation-labels'
import type { EvaluationRun, PlanningMethod, RoadLegCollection, TourRoute } from '@/types/api'

import { lineToLatLngs, toLatLng } from './geo'
import { depotIcon, ROUTE_CASING, stopIcon } from './map-theme'

type LineStyle = { color: string; weight: number; opacity: number }

/** One road-following route: every leg is a line along the road graph, not a straight segment. */
export function RoadRouteLine({
  legs,
  style,
  label,
  casing = false,
}: {
  legs: RoadLegCollection
  style: LineStyle
  label: string
  /** White outline under the line; only for a single route (it would hide overlapping ones). */
  casing?: boolean
}) {
  const positions = useMemo(
    () => legs.features.map((f) => lineToLatLngs(f.geometry.coordinates)),
    [legs],
  )
  return (
    <>
      {casing && (
        <Polyline
          positions={positions}
          interactive={false}
          pathOptions={{ ...ROUTE_CASING, weight: style.weight + 4, lineCap: 'round', lineJoin: 'round' }}
        />
      )}
      <Polyline
        positions={positions}
        pathOptions={{
          color: style.color,
          weight: style.weight,
          opacity: style.opacity,
          lineCap: 'round',
          lineJoin: 'round',
        }}
      >
        <Tooltip sticky>{label}</Tooltip>
      </Polyline>
    </>
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
    map.fitBounds(L.latLngBounds(positions), { paddingTopLeft: [56, 40], paddingBottomRight: [40, 96] })
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
      <Marker position={toLatLng(run.depot)} icon={depotIcon()} zIndexOffset={1000}>
        <Tooltip>Depot: {run.depot.name}</Tooltip>
      </Marker>
      {run.stops.map((stop) => {
        const position = numbering ? numbering.visitOrder.indexOf(stop.inputIndex) + 1 : stop.inputIndex
        return (
          <Marker
            key={stop.collectionPointId}
            position={toLatLng(stop)}
            icon={stopIcon(position, 'planned')}
            zIndexOffset={500}
          >
            <Popup>
              <div className="wm-popup">
                <div className="wm-popup-title">{stop.name}</div>
                <ul className="wm-popup-rows">
                  {feasible.map((r) => (
                    <li key={r.method}>
                      <span>{METHOD_LABELS[r.method]}</span>
                      <span>visit {r.visitOrder.indexOf(stop.inputIndex) + 1}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </Popup>
          </Marker>
        )
      })}
    </>
  )
}

/** The road route attached to the tour (accepted proposal or kept input order). */
export function TourRouteLayer({ route }: { route: TourRoute }) {
  const style =
    route.method === 'NN_PLUS_2_OPT'
      ? { color: '#16a34a', weight: 5, opacity: 0.95 }
      : METHOD_STYLES[route.method]
  return (
    <RoadRouteLine
      legs={route.legs}
      style={style}
      label={`Road route: ${METHOD_LABELS[route.method]}`}
      casing
    />
  )
}
