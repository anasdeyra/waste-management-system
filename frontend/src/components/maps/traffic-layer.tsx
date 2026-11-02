import { Polyline, Tooltip } from 'react-leaflet'

import { SPEED_BAND_LABELS, TRAFFIC_STATUS_LABELS, trafficLineStyle } from '@/lib/traffic-labels'
import type { TrafficLink } from '@/types/api'

import { lineToLatLngs } from './geo'

/** Display-only NYC DOT sensor links; render before the route and stop layers so they stay below. */
export function TrafficLayer({ links }: { links: TrafficLink[] }) {
  return (
    <>
      {links.map((l) => {
        const style = trafficLineStyle(l)
        return (
          <Polyline
            key={l.linkId}
            positions={lineToLatLngs(l.geometry.coordinates)}
            pathOptions={{ color: style.color, weight: style.weight, dashArray: style.dashArray, opacity: 0.85 }}
          >
            <Tooltip sticky>
              <div className="text-xs">
                <div className="font-medium">{l.name ?? `Link ${l.linkId}`}</div>
                <div>
                  {TRAFFIC_STATUS_LABELS[l.status]}
                  {l.speedBand ? ` (${SPEED_BAND_LABELS[l.speedBand]})` : ''}
                </div>
                {l.speedMph !== undefined && <div>{l.speedMph} mph</div>}
                <div>observed {l.dataAsOfLocal} NYC time</div>
                {l.geometryTruncated && <div>(geometry truncated by the source)</div>}
              </div>
            </Tooltip>
          </Polyline>
        )
      })}
    </>
  )
}
