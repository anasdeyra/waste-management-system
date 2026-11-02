import { CircleMarker, LayerGroup, LayersControl, Marker, Polyline, Popup, Tooltip } from 'react-leaflet'

import { buttonVariants } from '@/components/ui/button'

import { STOP_STATUS_LABELS } from '@/lib/execution-labels'
import type { CollectionPoint, Tour } from '@/types/api'

import { toLatLng } from './geo'
import { depotIcon, MARKER, pointRadius, stopIcon } from './map-theme'
import { useMapZoom } from './use-map-zoom'

export const INPUT_ORDER_LABEL = 'Input order (straight lines, not a route)'

/**
 * Tour planning layers: depot, candidate points, numbered stops and the input-order line.
 * The line joins stops in input order with straight segments; it is not a route (S5 adds
 * road routing). Markers come from map-theme.ts (no default marker images).
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
  const zoom = useMapZoom()
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
          <Marker position={toLatLng(tour.depot)} icon={depotIcon()} zIndexOffset={1000}>
            <Tooltip>Depot: {tour.depot.name}</Tooltip>
          </Marker>
        </LayerGroup>
      </LayersControl.Overlay>
      {/* Candidates only matter while stops can be added; locked tours start with them hidden. */}
      <LayersControl.Overlay checked={editable} name="Candidate points">
        <LayerGroup>
          {free.map((p) => (
            <CircleMarker
              key={p.id}
              center={toLatLng(p)}
              radius={Math.max(2.5, pointRadius(zoom) - 1)}
              pathOptions={{
                className: 'wm-point',
                color: MARKER.ring,
                weight: 1.5,
                fillColor: MARKER.candidate,
                fillOpacity: 0.85,
              }}
            >
              <Tooltip direction="top">{p.name}</Tooltip>
              <Popup>
                <div className="wm-popup">
                  <div className="wm-popup-title">{p.name}</div>
                  <div className="wm-popup-meta">Candidate · {p.totalDemandKg} kg demand</div>
                  {editable && (
                    <button
                      type="button"
                      className={buttonVariants({ size: 'sm', className: 'mt-1 w-full' })}
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
            const tone = tour.status === 'DRAFT' ? 'planned' : s.status
            return (
              <Marker
                key={s.id}
                position={toLatLng(s)}
                icon={stopIcon(s.visitOrder, tone, selected, flagged)}
                zIndexOffset={selected ? 900 : 500}
                eventHandlers={{ click: () => onSelectStop(s.id) }}
              >
                <Popup>
                  <div className="wm-popup">
                    <div className="wm-popup-title">
                      Stop {s.visitOrder} · {s.name}
                    </div>
                    {tour.status !== 'DRAFT' && (
                      <div className="wm-popup-meta">{STOP_STATUS_LABELS[s.status]}</div>
                    )}
                    {flagged && (
                      <div className="wm-popup-warn">Inactive or not eligible for optimization</div>
                    )}
                    <ul className="wm-popup-rows">
                      {s.plannedDemands.map((d) => (
                        <li key={d.id}>
                          <span>{d.categoryName}</span>
                          <span>{d.plannedQuantityKg} kg</span>
                        </li>
                      ))}
                      <li className="wm-popup-total">
                        <span>Planned total</span>
                        <span>{s.plannedTotalKg} kg</span>
                      </li>
                      {s.actualQuantities.map((q) => (
                        <li key={q.id}>
                          <span>Collected {q.categoryName}</span>
                          <span>{q.actualQuantityKg} kg</span>
                        </li>
                      ))}
                    </ul>
                    {s.incidentReason && <div className="wm-popup-warn">{s.incidentReason}</div>}
                    {editable && (
                      <button
                        type="button"
                        className={buttonVariants({ size: 'sm', variant: 'outline', className: 'mt-1 w-full' })}
                        onClick={() => onRemove(s.id)}
                      >
                        Remove from tour
                      </button>
                    )}
                  </div>
                </Popup>
              </Marker>
            )
          })}
        </LayerGroup>
      </LayersControl.Overlay>
      <LayersControl.Overlay checked name={INPUT_ORDER_LABEL}>
        <LayerGroup>
          {linePositions.length > 0 && (
            <Polyline
              positions={linePositions}
              pathOptions={{ color: MARKER.inputOrder, weight: 2, opacity: 0.75, dashArray: '2 6', lineCap: 'round' }}
            />
          )}
        </LayerGroup>
      </LayersControl.Overlay>
    </LayersControl>
  )
}
