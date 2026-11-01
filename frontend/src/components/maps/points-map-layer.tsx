import { CircleMarker, LayerGroup, LayersControl, Popup, Tooltip } from 'react-leaflet'

import type { CollectionPoint, Depot } from '@/types/api'

import { formatCoordinates, originLabel, toLatLng } from './geo'

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
        radius={selected ? 10 : 7}
        pathOptions={{
          color: selected ? '#2563eb' : isActive ? '#15803d' : '#6b7280',
          fillColor: isActive ? '#22c55e' : '#d1d5db',
          fillOpacity: 0.9,
          weight: selected ? 3 : 2,
          dashArray: isActive ? undefined : '3',
        }}
        eventHandlers={{ click: () => onSelect(p.id) }}
      >
        <Tooltip>{p.name}</Tooltip>
        <Popup>
          <div className="space-y-1 text-sm">
            <div className="font-semibold">{p.name}</div>
            <div>
              {isActive ? 'Active' : 'Inactive'} · {originLabel(p.origin)}
            </div>
            {src && <div>Source: {src}</div>}
            <div>{formatCoordinates(p)}</div>
            <ul>
              {p.demands.map((d) => (
                <li key={d.id}>
                  {d.categoryName}: {d.quantityKg} kg
                </li>
              ))}
            </ul>
            <div>Total: {p.totalDemandKg} kg</div>
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
              <CircleMarker
                key={d.id}
                center={toLatLng(d)}
                radius={10}
                pathOptions={{ color: '#7c3aed', fillColor: '#7c3aed', fillOpacity: 0.9 }}
              >
                <Tooltip>{d.name}</Tooltip>
                <Popup>
                  <div className="space-y-1 text-sm">
                    <div className="font-semibold">{d.name}</div>
                    <div>Depot · {originLabel(d.origin)}</div>
                    {src && <div>Source: {src}</div>}
                    <div>{formatCoordinates(d)}</div>
                  </div>
                </Popup>
              </CircleMarker>
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
