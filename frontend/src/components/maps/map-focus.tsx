import { useEffect } from 'react'
import { useMap } from 'react-leaflet'

import { toLatLng, type LonLat } from './geo'

/** Flies the map to a target position whenever its coordinates change. */
export function MapFocus({ target }: { target: LonLat | null }) {
  const map = useMap()
  const longitude = target?.longitude
  const latitude = target?.latitude
  useEffect(() => {
    if (longitude === undefined || latitude === undefined) return
    map.flyTo(toLatLng({ longitude, latitude }), Math.max(map.getZoom(), 17))
  }, [map, longitude, latitude])
  return null
}
