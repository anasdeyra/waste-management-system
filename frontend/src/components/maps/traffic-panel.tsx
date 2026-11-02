import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import {
  formatAge,
  SPEED_BAND_COLORS,
  SPEED_BAND_LABELS,
} from '@/lib/traffic-labels'
import type { SpeedBand, TrafficOverlay } from '@/types/api'

interface Props {
  enabled: boolean
  onToggle: (on: boolean) => void
  loading: boolean
  overlay?: TrafficOverlay
  error?: Error | null
}

function Swatch({ color, dashed }: { color: string; dashed?: boolean }) {
  return (
    <span
      className="inline-block h-0 w-6 border-t-4"
      style={{ borderColor: color, borderStyle: dashed ? 'dashed' : 'solid' }}
    />
  )
}

/** Switch and information panel of the display-only traffic layer, shown under a map. */
export function TrafficPanel({ enabled, onToggle, loading, overlay, error }: Props) {
  return (
    <div className="flex flex-col gap-2 text-sm">
      <div className="flex items-center gap-2">
        <Switch id="traffic-switch" checked={enabled} onCheckedChange={onToggle} />
        <Label htmlFor="traffic-switch">NYC DOT traffic (display only)</Label>
      </div>
      {enabled && loading && <Skeleton className="h-16" />}
      {enabled && error && (
        <Alert variant="destructive">
          <AlertTitle>Traffic snapshot unavailable</AlertTitle>
          <AlertDescription>{error.message}</AlertDescription>
        </Alert>
      )}
      {enabled && overlay && !overlay.available && (
        <Alert>
          <AlertTitle>Traffic snapshot unavailable</AlertTitle>
          <AlertDescription>
            {overlay.unavailableReason} {overlay.displayOnlyNote}
          </AlertDescription>
        </Alert>
      )}
      {enabled && overlay?.available && (
        <div className="flex flex-col gap-2 rounded-md border p-3">
          <div className="flex flex-wrap items-center gap-2">
            <span>
              Observed {overlay.observedAtLocal} NYC time ({overlay.observedAt} UTC)
            </span>
            <Badge variant="secondary">Stored snapshot, not live traffic</Badge>
            {overlay.ageMinutes !== undefined && <span>{formatAge(overlay.ageMinutes)} old</span>}
          </div>
          <p>
            {overlay.counts.observed} observed · {overlay.counts.stale} stale · {overlay.counts.noData} without a valid
            reading · {overlay.counts.invalid} invalid
          </p>
          <ul className="flex flex-wrap gap-4 text-xs">
            {(Object.keys(SPEED_BAND_LABELS) as SpeedBand[]).map((b) => (
              <li key={b} className="flex items-center gap-1">
                <Swatch color={SPEED_BAND_COLORS[b]} />
                {SPEED_BAND_LABELS[b]}
              </li>
            ))}
            <li className="flex items-center gap-1">
              <Swatch color="#6b7280" dashed />
              Stale reading
            </li>
            <li className="flex items-center gap-1">
              <Swatch color="#9ca3af" dashed />
              No valid reading
            </li>
          </ul>
          <p className="text-xs text-muted-foreground">{overlay.coverageNote}</p>
          <p className="text-xs text-muted-foreground">{overlay.displayOnlyNote}</p>
          <p className="text-xs text-muted-foreground">{overlay.attribution}</p>
          <details className="text-xs text-muted-foreground">
            <summary className="cursor-pointer">NYC DOT disclaimer</summary>
            <p className="mt-1">{overlay.disclaimer}</p>
          </details>
        </div>
      )}
    </div>
  )
}
