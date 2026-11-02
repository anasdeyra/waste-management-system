import { LayerGroup, LayersControl, Marker, Popup, Tooltip } from 'react-leaflet'

import { STOP_STATUS_LABELS } from '@/lib/execution-labels'
import type { Tour } from '@/types/api'

import { toLatLng } from './geo'
import { depotIcon, stopIcon } from './map-theme'

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
          <Marker position={toLatLng(tour.depot)} icon={depotIcon()} zIndexOffset={1000}>
            <Tooltip>Depot: {tour.depot.name}</Tooltip>
          </Marker>
        </LayerGroup>
      </LayersControl.Overlay>
      <LayersControl.Overlay checked name="Tour stops">
        <LayerGroup>
          {tour.stops.map((s) => (
            <Marker
              key={s.id}
              position={toLatLng(s)}
              icon={stopIcon(s.visitOrder, s.status, s.id === selectedStopId)}
              zIndexOffset={s.id === selectedStopId ? 900 : 500}
              eventHandlers={{ click: () => onSelectStop(s.id) }}
            >
              <Popup>
                <div className="wm-popup">
                  <div className="wm-popup-title">
                    Stop {s.visitOrder} · {s.name}
                  </div>
                  <div className="wm-popup-meta">{STOP_STATUS_LABELS[s.status]}</div>
                  <ul className="wm-popup-rows">
                    {s.plannedDemands.map((d) => (
                      <li key={d.id}>
                        <span>Planned {d.categoryName}</span>
                        <span>{d.plannedQuantityKg} kg</span>
                      </li>
                    ))}
                    {s.actualQuantities.map((q) => (
                      <li key={q.id}>
                        <span>Collected {q.categoryName}</span>
                        <span>{q.actualQuantityKg} kg</span>
                      </li>
                    ))}
                  </ul>
                  {s.incidentReason && <div className="wm-popup-warn">{s.incidentReason}</div>}
                </div>
              </Popup>
            </Marker>
          ))}
        </LayerGroup>
      </LayersControl.Overlay>
    </LayersControl>
  )
}
