import { CircleMarker, LayerGroup, LayersControl, Marker, Popup, Tooltip } from 'react-leaflet'

import type { CollectionPoint, Depot } from '@/types/api'

import { formatCoordinates, originLabel, toLatLng } from './geo'
import { depotIcon, pointPathOptions, pointRadius } from './map-theme'
import { useMapZoom } from './use-map-zoom'

function source(dataset?: string, identifier?: string, ownership?: string): string | null {
  if (!dataset && !identifier) return null
  const base = [dataset, identifier].filter(Boolean).join(' / ')
  return ownership ? `${base} · ${ownership}` : base
}

/** Depot, active and inactive collection points as toggleable layers (FR-22). */
export function PointsMapLayer({
  points,
  depots,
  selectedId,
  onSelect,
}: {
  points: CollectionPoint[]
  depots: Depot[]
  selectedId: string | null
  onSelect: (id: string) => void
}) {
  const zoom = useMapZoom()
  const active = points.filter((p) => p.status === 'ACTIVE')
  const inactive = points.filter((p) => p.status === 'INACTIVE')

  const renderPoint = (p: CollectionPoint) => {
    const selected = p.id === selectedId
    const isActive = p.status === 'ACTIVE'
    const src = source(p.sourceDataset, p.sourceIdentifier, p.sourceOwnershipType)
    return (
      <CircleMarker
        key={p.id}
        center={toLatLng(p)}
        radius={pointRadius(zoom, selected)}
        pathOptions={pointPathOptions(isActive, selected)}
        eventHandlers={{ click: () => onSelect(p.id) }}
      >
        <Tooltip direction="top" offset={[0, -4]}>
          {p.name}
        </Tooltip>
        <Popup>
          <div className="wm-popup">
            <div className="wm-popup-title">{p.name}</div>
            <div className="wm-popup-meta">
              {isActive ? 'Active' : 'Inactive'} · {originLabel(p.origin)}
            </div>
            {p.demands.length > 0 && (
              <ul className="wm-popup-rows">
                {p.demands.map((d) => (
                  <li key={d.id}>
                    <span>{d.categoryName}</span>
                    <span>{d.quantityKg} kg</span>
                  </li>
                ))}
                <li className="wm-popup-total">
                  <span>Total</span>
                  <span>{p.totalDemandKg} kg</span>
                </li>
              </ul>
            )}
            {src && <div className="wm-popup-fine">Source: {src}</div>}
            <div className="wm-popup-fine">{formatCoordinates(p)}</div>
          </div>
        </Popup>
      </CircleMarker>
    )
  }

  return (
    <LayersControl position="topright">
      <LayersControl.Overlay checked name="Depot">
        <LayerGroup>
          {depots.map((d) => {
            const src = source(d.sourceDataset, d.sourceIdentifier)
            return (
              <Marker key={d.id} position={toLatLng(d)} icon={depotIcon()} zIndexOffset={1000}>
                <Tooltip>{d.name}</Tooltip>
                <Popup>
                  <div className="wm-popup">
                    <div className="wm-popup-title">{d.name}</div>
                    <div className="wm-popup-meta">Depot · {originLabel(d.origin)}</div>
                    {src && <div className="wm-popup-fine">Source: {src}</div>}
                    <div className="wm-popup-fine">{formatCoordinates(d)}</div>
                  </div>
                </Popup>
              </Marker>
            )
          })}
        </LayerGroup>
      </LayersControl.Overlay>
      <LayersControl.Overlay checked name="Active points">
        <LayerGroup>{active.map(renderPoint)}</LayerGroup>
      </LayersControl.Overlay>
      <LayersControl.Overlay checked name="Inactive points">
        <LayerGroup>{inactive.map(renderPoint)}</LayerGroup>
      </LayersControl.Overlay>
    </LayersControl>
  )
}
