import { Marker, Tooltip } from 'react-leaflet'

import { toLatLng, type LonLat } from './geo'
import { pickedIcon } from './map-theme'

/** Marks a location picked on the map but not yet saved. */
export function PickedLocationLayer({ location }: { location: LonLat | null }) {
  if (!location) return null
  return (
    <Marker position={toLatLng(location)} icon={pickedIcon()} zIndexOffset={2000}>
      <Tooltip permanent direction="top">
        New location (not saved)
      </Tooltip>
    </Marker>
  )
}
