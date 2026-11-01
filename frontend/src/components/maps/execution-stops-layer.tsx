import { CircleMarker, LayerGroup, LayersControl, Popup, Tooltip } from 'react-leaflet'

import { STOP_STATUS_COLORS, STOP_STATUS_LABELS } from '@/lib/execution-labels'
import type { Tour } from '@/types/api'

import { toLatLng } from './geo'

/** Execution map layers: depot and stops numbered in visit order, coloured by outcome. */
export function ExecutionStopsLayer({
  tour,
  selectedStopId,
  onSelectStop,
}: {
  tour: Tour
  selectedStopId: string | null
  onSelectStop: (id: string) => void
}) {
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
      <LayersControl.Overlay checked name="Tour stops">
        <LayerGroup>
          {tour.stops.map((s) => (
            <CircleMarker
              key={s.id}
              center={toLatLng(s)}
              radius={9}
              pathOptions={{
                color: STOP_STATUS_COLORS[s.status],
                fillColor: STOP_STATUS_COLORS[s.status],
                fillOpacity: 0.85,
                weight: s.id === selectedStopId ? 4 : 2,
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
                  <div>{STOP_STATUS_LABELS[s.status]}</div>
                  <ul>
                    {s.plannedDemands.map((d) => (
                      <li key={d.id}>
                        Planned {d.categoryName}: {d.plannedQuantityKg} kg
                      </li>
                    ))}
                  </ul>
                  {s.actualQuantities.length > 0 && (
                    <ul>
                      {s.actualQuantities.map((q) => (
                        <li key={q.id}>
                          Collected {q.categoryName}: {q.actualQuantityKg} kg
                        </li>
                      ))}
                    </ul>
                  )}
                  {s.incidentReason && <div>{s.incidentReason}</div>}
                </div>
              </Popup>
            </CircleMarker>
          ))}
        </LayerGroup>
      </LayersControl.Overlay>
    </LayersControl>
  )
}
