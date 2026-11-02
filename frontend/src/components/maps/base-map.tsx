import 'leaflet/dist/leaflet.css'
import 'maplibre-gl/dist/maplibre-gl.css'

import L from 'leaflet'
import '@maplibre/maplibre-gl-leaflet'
import { setWorkerUrl } from 'maplibre-gl'
import maplibreWorkerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url'
import { useEffect, useRef, type ReactNode } from 'react'
import { MapContainer, ScaleControl, useMap, useMapEvents } from 'react-leaflet'

import { cn } from '@/lib/utils'

import { BASEMAP_STYLE } from './basemap-style'
import {
  DEFAULT_ZOOM,
  MANHATTAN_BOUNDS,
  MIN_ZOOM,
  MN01_CENTER,
  toLatLng,
  type LonLat,
} from './geo'

const MAX_BOUNDS = L.latLngBounds(
  toLatLng(MANHATTAN_BOUNDS.southWest),
  toLatLng(MANHATTAN_BOUNDS.northEast),
)
import type { LegendItem } from './legend-items'
import { MapLegend } from './map-legend'

// Vite pre-bundles maplibre-gl, which breaks its relative worker URL; hand it the bundled worker.
setWorkerUrl(maplibreWorkerUrl)

function MapClickHandler({ onMapClick }: { onMapClick?: (p: LonLat) => void }) {
  useMapEvents({
    click: (e) => onMapClick?.({ longitude: e.latlng.lng, latitude: e.latlng.lat }),
  })
  return null
}

/** Minimal vector basemap (OpenFreeMap, OpenStreetMap data) drawn by MapLibre under the Leaflet layers. */
function VectorBasemap() {
  const map = useMap()
  useEffect(() => {
    const layer = L.maplibreGL({ style: BASEMAP_STYLE })
    layer.addTo(map)
    return () => {
      layer.remove()
    }
  }, [map])
  return null
}

// Lucide "house" and "scan" glyphs for the camera buttons.
const HOME_SVG =
  '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>'
const FIT_SVG =
  '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 7V5a2 2 0 0 1 2-2h2"/><path d="M17 3h2a2 2 0 0 1 2 2v2"/><path d="M21 17v2a2 2 0 0 1-2 2h-2"/><path d="M7 21H5a2 2 0 0 1-2-2v-2"/></svg>'

// Room for the controls (top) and the legend card (bottom) when framing data.
const FIT_PADDING: L.FitBoundsOptions = { paddingTopLeft: [56, 40], paddingBottomRight: [40, 96] }

function addButton(bar: HTMLElement, svg: string, label: string, onClick: () => void) {
  const button = L.DomUtil.create('a', '', bar)
  button.href = '#'
  button.title = label
  button.setAttribute('role', 'button')
  button.setAttribute('aria-label', label)
  button.innerHTML = svg
  L.DomEvent.on(button, 'click', (e) => {
    L.DomEvent.preventDefault(e)
    onClick()
  })
}

/**
 * Camera buttons under the zoom control. "Reset view" returns to the initial camera (the data
 * framing when `fitTo` is given, else the MN01 default); "Fit to data" frames the current data.
 * The data framing is applied once, when the positions first arrive.
 */
function CameraControls({ positions }: { positions?: LonLat[] }) {
  const map = useMap()
  const latest = useRef(positions)
  useEffect(() => {
    latest.current = positions
  }, [positions])
  const home = useRef<{ center: L.LatLng; zoom: number } | null>(null)
  const hasData = positions !== undefined

  useEffect(() => {
    const fitData = (animate: boolean) => {
      const pts = latest.current ?? []
      if (pts.length === 0) return false
      if (pts.length === 1) map.setView(toLatLng(pts[0]), Math.max(map.getZoom(), 16), { animate })
      else map.fitBounds(L.latLngBounds(pts.map(toLatLng)), { ...FIT_PADDING, maxZoom: 17, animate })
      return true
    }
    const control = new L.Control({ position: 'topleft' })
    control.onAdd = () => {
      const bar = L.DomUtil.create('div', 'leaflet-bar wm-camera-control')
      L.DomEvent.disableClickPropagation(bar)
      addButton(bar, HOME_SVG, 'Reset view', () => {
        const target = home.current ?? { center: L.latLng(toLatLng(MN01_CENTER)), zoom: DEFAULT_ZOOM }
        map.flyTo(target.center, target.zoom, { duration: 0.6 })
      })
      if (hasData) addButton(bar, FIT_SVG, 'Fit map to data', () => fitData(true))
      return bar
    }
    control.addTo(map)
    return () => {
      control.remove()
    }
  }, [map, hasData])

  useEffect(() => {
    if (home.current || !positions || positions.length === 0) return
    const pts = positions
    if (pts.length === 1) map.setView(toLatLng(pts[0]), 16, { animate: false })
    else map.fitBounds(L.latLngBounds(pts.map(toLatLng)), { ...FIT_PADDING, maxZoom: 17, animate: false })
    home.current = { center: map.getCenter(), zoom: map.getZoom() }
  }, [map, positions])
  return null
}

/**
 * Leaflet map on a custom minimal OpenStreetMap vector style with the required attribution
 * (D-054), limited to Manhattan. `fitTo` frames the data on first load and powers the fit
 * button; `legend` explains the marker shapes.
 */
export function BaseMap({
  children,
  className,
  onMapClick,
  fitTo,
  legend,
}: {
  children?: ReactNode
  className?: string
  onMapClick?: (p: LonLat) => void
  fitTo?: LonLat[]
  legend?: LegendItem[]
}) {
  return (
    // isolate + z-0 keeps Leaflet panes (z-index 400+) below dialogs and toasts.
    <div
      className={cn(
        'wm-map relative isolate z-0 h-[480px] w-full overflow-hidden rounded-xl border shadow-sm',
        onMapClick && 'wm-map-pickable',
        className,
      )}
    >
      <MapContainer
        center={toLatLng(MN01_CENTER)}
        zoom={DEFAULT_ZOOM}
        minZoom={MIN_ZOOM}
        maxZoom={19}
        maxBounds={MAX_BOUNDS}
        maxBoundsViscosity={1}
        scrollWheelZoom
        zoomSnap={0.5}
        className="h-full w-full"
      >
        <VectorBasemap />
        <ScaleControl position="bottomright" imperial metric />
        <CameraControls positions={fitTo} />
        <MapClickHandler onMapClick={onMapClick} />
        {children}
      </MapContainer>
      {legend && <MapLegend items={legend} />}
    </div>
  )
}
