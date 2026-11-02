import { useState } from 'react'
import { useMap, useMapEvents } from 'react-leaflet'

/** Current zoom level of the surrounding Leaflet map, updated after each zoom. */
export function useMapZoom(): number {
  const map = useMap()
  const [zoom, setZoom] = useState(() => map.getZoom())
  useMapEvents({ zoomend: () => setZoom(map.getZoom()) })
  return zoom
}
