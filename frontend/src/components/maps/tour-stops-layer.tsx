import { CircleMarker, LayerGroup, LayersControl, Polyline, Popup, Tooltip } from 'react-leaflet'

import { STOP_STATUS_COLORS, STOP_STATUS_LABELS } from '@/lib/execution-labels'
import type { CollectionPoint, Tour } from '@/types/api'

import { toLatLng } from './geo'

export const INPUT_ORDER_LABEL = 'Input order (straight lines, not a route)'

/**
 * Tour planning layers: depot, candidate points, numbered stops and the input-order line.
 * The line joins stops in input order with straight segments; it is not a route (S5 adds
 * road routing). Only circle markers are used (no default marker images).
 */
export function TourStopsLayer({
  tour,
  candidates,
  selectedStopId,
  editable,
  onAdd,
  onRemove,
  onSelectStop,
}: {
  tour: Tour
  candidates: CollectionPoint[]
  selectedStopId: string | null
  editable: boolean
  onAdd: (pointId: string) => void
  onRemove: (stopId: string) => void
  onSelectStop: (stopId: string) => void
}) {
  const inTour = new Set(tour.stops.map((s) => s.collectionPointId))
  const free = candidates.filter((p) => !inTour.has(p.id))
  const linePositions =
    tour.stops.length === 0
      ? []
      : [
          toLatLng(tour.depot),
          ...tour.stops.map((s) => toLatLng(s)),
          toLatLng(tour.depot),
        ]

  return (
    <LayersControl position="topright">
      <LayersControl.Overlay checked name="Depot">
        <LayerGroup>
          <CircleMarker
            center={toLatLng(tour.depot)}
            radius={10}
            pathOptions={{ color: '#7c3aed', fillColor: '#7c3aed', fillOpacity: 0.9 }}
          >
            <Tooltip>Depot: {tour.depot.name}</Tooltip>
          </CircleMarker>
        </LayerGroup>
      </LayersControl.Overlay>
      <LayersControl.Overlay checked name="Candidate points">
        <LayerGroup>
          {free.map((p) => (
            <CircleMarker
              key={p.id}
              center={toLatLng(p)}
              radius={5}
              pathOptions={{ color: '#4d7c0f', fillColor: '#9ca3af', fillOpacity: 0.8, weight: 1 }}
            >
              <Popup>
                <div className="space-y-1 text-sm">
                  <div className="font-semibold">{p.name}</div>
                  <div>Total demand: {p.totalDemandKg} kg</div>
                  {editable && (
                    <button
                      type="button"
                      className="rounded border px-2 py-1 text-sm font-medium"
                      onClick={() => onAdd(p.id)}
                    >
                      Add to tour
                    </button>
                  )}
                </div>
              </Popup>
            </CircleMarker>
          ))}
        </LayerGroup>
      </LayersControl.Overlay>
      <LayersControl.Overlay checked name="Tour stops">
        <LayerGroup>
          {tour.stops.map((s) => {
            const flagged = s.pointStatus !== 'ACTIVE' || !s.optimizationEligible
            const selected = s.id === selectedStopId
            const executionColor = tour.status === 'DRAFT' ? null : STOP_STATUS_COLORS[s.status]
            return (
              <CircleMarker
                key={s.id}
                center={toLatLng(s)}
                radius={9}
                pathOptions={{
                  color: flagged ? '#dc2626' : (executionColor ?? '#1d4ed8'),
                  fillColor: executionColor ?? '#3b82f6',
                  fillOpacity: 0.9,
                  weight: selected ? 4 : 2,
                  dashArray: flagged ? '4 3' : undefined,
                }}
                eventHandlers={{ click: () => onSelectStop(s.id) }}
              >
                <Tooltip permanent direction="center" className="tour-stop-number">
                  {s.visitOrder}
                </Tooltip>
                <Popup>
                  <div className="space-y-1 text-sm">
                    <div className="font-semibold">
                      Stop {s.visitOrder} · {s.name}
                    </div>
                    <ul>
                      {s.plannedDemands.map((d) => (
                        <li key={d.id}>
                          {d.categoryName}: {d.plannedQuantityKg} kg
                        </li>
                      ))}
                    </ul>
                    <div>Planned total: {s.plannedTotalKg} kg</div>
                    {tour.status !== 'DRAFT' && <div>{STOP_STATUS_LABELS[s.status]}</div>}
                    {s.actualQuantities.map((q) => (
                      <div key={q.id}>
                        Collected {q.categoryName}: {q.actualQuantityKg} kg
                      </div>
                    ))}
                    {s.incidentReason && <div>{s.incidentReason}</div>}
                    {editable && (
                      <button
                        type="button"
                        className="rounded border px-2 py-1 text-sm font-medium"
                        onClick={() => onRemove(s.id)}
                      >
                        Remove from tour
                      </button>
                    )}
                  </div>
                </Popup>
              </CircleMarker>
            )
          })}
        </LayerGroup>
      </LayersControl.Overlay>
      <LayersControl.Overlay checked name={INPUT_ORDER_LABEL}>
        <LayerGroup>
          {linePositions.length > 0 && (
            <Polyline
              positions={linePositions}
              pathOptions={{ color: '#4b5563', weight: 2, opacity: 0.7, dashArray: '6 6' }}
            />
          )}
        </LayerGroup>
      </LayersControl.Overlay>
    </LayersControl>
  )
}
