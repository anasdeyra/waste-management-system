import { originLabel } from '@/components/maps/geo'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import type { CollectionPoint, WasteCategory } from '@/types/api'

import { DemandEditor } from './demand-editor'

/** Details of the selected point with edit and activate/deactivate actions. */
export function PointDetails({
  point,
  categories,
  busy,
  onEdit,
  onToggleStatus,
}: {
  point: CollectionPoint
  categories: WasteCategory[]
  busy: boolean
  onEdit: () => void
  onToggleStatus: () => void
}) {
  const active = point.status === 'ACTIVE'
  return (
    <Card>
      <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-2">
        <CardTitle className="flex items-center gap-2">
          {point.name}
          <Badge variant={active ? 'default' : 'secondary'}>{active ? 'Active' : 'Inactive'}</Badge>
        </CardTitle>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={onEdit}>
            Edit
          </Button>
          <Button variant="outline" size="sm" disabled={busy} onClick={onToggleStatus}>
            {active ? 'Deactivate' : 'Activate'}
          </Button>
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
          <dt className="text-muted-foreground">Origin</dt>
          <dd>{originLabel(point.origin)}</dd>
          <dt className="text-muted-foreground">Source dataset</dt>
          <dd>{point.sourceDataset ?? '—'}</dd>
          <dt className="text-muted-foreground">Source identifier</dt>
          <dd>{point.sourceIdentifier ?? '—'}</dd>
          <dt className="text-muted-foreground">Source ownership type</dt>
          <dd>{point.sourceOwnershipType ?? '—'}</dd>
          <dt className="text-muted-foreground">Longitude</dt>
          <dd>{point.longitude.toFixed(6)}</dd>
          <dt className="text-muted-foreground">Latitude</dt>
          <dd>{point.latitude.toFixed(6)}</dd>
          <dt className="text-muted-foreground">Total demand</dt>
          <dd>{point.totalDemandKg} kg</dd>
        </dl>
        <DemandEditor point={point} categories={categories} />
      </CardContent>
    </Card>
  )
}
