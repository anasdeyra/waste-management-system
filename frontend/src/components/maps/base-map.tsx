import 'leaflet/dist/leaflet.css'

import type { ReactNode } from 'react'
import { MapContainer, TileLayer, useMapEvents } from 'react-leaflet'

import { cn } from '@/lib/utils'

import { MN01_CENTER, toLatLng, type LonLat } from './geo'

function MapClickHandler({ onMapClick }: { onMapClick?: (p: LonLat) => void }) {
  useMapEvents({
    click: (e) => onMapClick?.({ longitude: e.latlng.lng, latitude: e.latlng.lat }),
  })
  return null
}

/** Leaflet map on OpenStreetMap tiles with the required attribution (D-039). */
export function BaseMap({
  children,
  className,
  onMapClick,
}: {
  children?: ReactNode
  className?: string
  onMapClick?: (p: LonLat) => void
}) {
  return (
    // isolate + z-0 keeps Leaflet panes (z-index 400+) below dialogs and toasts.
    <div
      className={cn('relative isolate z-0 h-[480px] w-full overflow-hidden rounded-md border', className)}
    >
      <MapContainer center={toLatLng(MN01_CENTER)} zoom={15} scrollWheelZoom className="h-full w-full">
        <TileLayer
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          maxZoom={19}
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        />
        <MapClickHandler onMapClick={onMapClick} />
        {children}
      </MapContainer>
    </div>
  )
}
