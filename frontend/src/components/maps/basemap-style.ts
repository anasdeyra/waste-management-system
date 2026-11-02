import type { StyleSpecification, SymbolLayerSpecification } from 'maplibre-gl'

/**
 * Minimal basemap style on OpenFreeMap vector tiles (OpenMapTiles schema, OpenStreetMap data).
 * No businesses, house numbers or transit icons; only neighbourhood names and major street
 * names. Roads stay neutral so the coloured traffic, route and stop layers keep their contrast;
 * parks and water keep their natural colours.
 */
const C = {
  land: '#f5f4f0',
  landuse: '#efede7',
  building: '#e4e1da',
  buildingLine: '#d6d2c9',
  park: '#cfe8c4',
  wood: '#b9dcae',
  water: '#a3cdea',
  waterLabel: '#3b6f96',
  roadCasing: '#dcd8cf',
  road: '#ffffff',
  majorCasing: '#cbc6bc',
  rail: '#c9c4bb',
  label: '#5b5852',
  placeLabel: '#3f3c37',
  halo: '#ffffff',
  boundary: '#b9b4ab',
} as const

const FONT = ['Noto Sans Regular']
const FONT_BOLD = ['Noto Sans Bold']
const FONT_ITALIC = ['Noto Sans Italic']

const MAJOR = ['motorway', 'trunk', 'primary', 'secondary']
const MINOR = ['tertiary', 'minor']

/** Road width by zoom: [z12, z15, z18]. */
function width(w12: number, w15: number, w18: number) {
  return ['interpolate', ['exponential', 1.5], ['zoom'], 12, w12, 15, w15, 18, w18] as never
}

const roadFilter = (classes: string[]) =>
  ['all', ['match', ['get', 'class'], classes, true, false], ['!=', ['get', 'brunnel'], 'tunnel']] as never

function waterName(id: string, placement: 'point' | 'line'): SymbolLayerSpecification {
  return {
    id,
    type: 'symbol',
    source: 'openmaptiles',
    'source-layer': 'water_name',
    // Only large water bodies (rivers, bays, the sea); no pools, ponds or marinas.
    filter: [
      'all',
      ['==', ['geometry-type'], placement === 'line' ? 'LineString' : 'Point'],
      ['match', ['get', 'class'], ['ocean', 'sea', 'bay', 'strait', 'river'], true, false],
    ],
    layout: {
      'symbol-placement': placement,
      'text-field': ['coalesce', ['get', 'name:en'], ['get', 'name']],
      'text-font': FONT_ITALIC,
      'text-size': 12,
      'text-letter-spacing': 0.1,
    },
    paint: { 'text-color': C.waterLabel, 'text-halo-color': C.water, 'text-halo-width': 1 },
  }
}

export const BASEMAP_STYLE: StyleSpecification = {
  version: 8,
  glyphs: 'https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf',
  sources: {
    openmaptiles: {
      type: 'vector',
      url: 'https://tiles.openfreemap.org/planet',
      attribution:
        '<a href="https://openfreemap.org" target="_blank">OpenFreeMap</a> &copy; <a href="https://www.openstreetmap.org/copyright" target="_blank">OpenStreetMap</a> contributors',
    },
  },
  layers: [
    { id: 'background', type: 'background', paint: { 'background-color': C.land } },
    {
      id: 'landuse',
      type: 'fill',
      source: 'openmaptiles',
      'source-layer': 'landuse',
      filter: ['match', ['get', 'class'], ['residential', 'commercial', 'industrial', 'retail'], true, false],
      paint: { 'fill-color': C.landuse },
    },
    {
      id: 'landcover-grass',
      type: 'fill',
      source: 'openmaptiles',
      'source-layer': 'landcover',
      filter: ['match', ['get', 'class'], ['grass', 'farmland', 'wetland'], true, false],
      paint: { 'fill-color': C.park },
    },
    {
      id: 'landcover-wood',
      type: 'fill',
      source: 'openmaptiles',
      'source-layer': 'landcover',
      filter: ['==', ['get', 'class'], 'wood'],
      paint: { 'fill-color': C.wood },
    },
    {
      id: 'park',
      type: 'fill',
      source: 'openmaptiles',
      'source-layer': 'park',
      paint: { 'fill-color': C.park },
    },
    {
      id: 'water',
      type: 'fill',
      source: 'openmaptiles',
      'source-layer': 'water',
      filter: ['!=', ['get', 'brunnel'], 'tunnel'],
      paint: { 'fill-color': C.water },
    },
    {
      id: 'waterway',
      type: 'line',
      source: 'openmaptiles',
      'source-layer': 'waterway',
      paint: { 'line-color': C.water, 'line-width': width(0.5, 1.5, 3) },
    },
    {
      id: 'building',
      type: 'fill',
      source: 'openmaptiles',
      'source-layer': 'building',
      minzoom: 14,
      paint: {
        'fill-color': C.building,
        'fill-outline-color': C.buildingLine,
        'fill-opacity': ['interpolate', ['linear'], ['zoom'], 14, 0, 15, 1],
      },
    },
    {
      id: 'rail',
      type: 'line',
      source: 'openmaptiles',
      'source-layer': 'transportation',
      filter: ['all', ['==', ['get', 'class'], 'rail'], ['!=', ['get', 'brunnel'], 'tunnel']],
      paint: { 'line-color': C.rail, 'line-width': 1, 'line-dasharray': [3, 2] },
    },
    {
      id: 'road-minor-casing',
      type: 'line',
      source: 'openmaptiles',
      'source-layer': 'transportation',
      minzoom: 13,
      filter: roadFilter(MINOR),
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': C.roadCasing, 'line-width': width(1, 5, 18) },
    },
    {
      id: 'road-major-casing',
      type: 'line',
      source: 'openmaptiles',
      'source-layer': 'transportation',
      filter: roadFilter(MAJOR),
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': C.majorCasing, 'line-width': width(2, 8, 26) },
    },
    {
      id: 'road-minor',
      type: 'line',
      source: 'openmaptiles',
      'source-layer': 'transportation',
      minzoom: 13,
      filter: roadFilter(MINOR),
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': C.road, 'line-width': width(0.5, 3.5, 15) },
    },
    {
      id: 'road-major',
      type: 'line',
      source: 'openmaptiles',
      'source-layer': 'transportation',
      filter: roadFilter(MAJOR),
      layout: { 'line-cap': 'round', 'line-join': 'round' },
      paint: { 'line-color': C.road, 'line-width': width(1, 6, 22) },
    },
    {
      id: 'boundary',
      type: 'line',
      source: 'openmaptiles',
      'source-layer': 'boundary',
      filter: ['<=', ['get', 'admin_level'], 4],
      paint: { 'line-color': C.boundary, 'line-width': 1, 'line-dasharray': [4, 3] },
    },
    waterName('water-name-point', 'point'),
    waterName('water-name-line', 'line'),
    {
      id: 'road-name-major',
      type: 'symbol',
      source: 'openmaptiles',
      'source-layer': 'transportation_name',
      minzoom: 14,
      filter: ['match', ['get', 'class'], MAJOR, true, false],
      layout: {
        'symbol-placement': 'line',
        'symbol-spacing': 400,
        'text-field': ['coalesce', ['get', 'name:en'], ['get', 'name']],
        'text-font': FONT,
        'text-size': ['interpolate', ['linear'], ['zoom'], 14, 10, 18, 13] as never,
      },
      paint: { 'text-color': C.label, 'text-halo-color': C.halo, 'text-halo-width': 1.5 },
    },
    {
      id: 'road-name-minor',
      type: 'symbol',
      source: 'openmaptiles',
      'source-layer': 'transportation_name',
      minzoom: 17,
      filter: ['match', ['get', 'class'], MINOR, true, false],
      layout: {
        'symbol-placement': 'line',
        'symbol-spacing': 500,
        'text-field': ['coalesce', ['get', 'name:en'], ['get', 'name']],
        'text-font': FONT,
        'text-size': 10,
      },
      paint: { 'text-color': C.label, 'text-halo-color': C.halo, 'text-halo-width': 1.5 },
    },
    {
      id: 'place-name',
      type: 'symbol',
      source: 'openmaptiles',
      'source-layer': 'place',
      filter: ['match', ['get', 'class'], ['city', 'town', 'suburb', 'neighbourhood'], true, false],
      layout: {
        'text-field': ['coalesce', ['get', 'name:en'], ['get', 'name']],
        'text-font': FONT_BOLD,
        'text-size': ['match', ['get', 'class'], ['city', 'town'], 14, 11] as never,
        'text-transform': 'uppercase',
        'text-letter-spacing': 0.12,
        'text-max-width': 8,
      },
      paint: { 'text-color': C.placeLabel, 'text-opacity': 0.75, 'text-halo-color': C.halo, 'text-halo-width': 1.5 },
    },
  ],
}
