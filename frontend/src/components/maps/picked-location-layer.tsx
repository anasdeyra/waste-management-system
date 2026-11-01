import { CircleMarker, Tooltip } from 'react-leaflet'

import { toLatLng, type LonLat } from './geo'

/** Marks a location picked on the map but not yet saved. */
export function PickedLocationLayer({ location }: { location: LonLat | null }) {
  if (!location) return null
  return (
    <CircleMarker
      center={toLatLng(location)}
      radius={9}
      pathOptions={{ color: '#ea580c', fillColor: '#fdba74', fillOpacity: 0.6, dashArray: '4' }}
    >
      <Tooltip permanent>New location (not saved)</Tooltip>
    </CircleMarker>
  )
}
