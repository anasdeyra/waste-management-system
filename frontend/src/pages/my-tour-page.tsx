import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { ExecutionStopList } from '@/components/execution/execution-stop-list'
import { TourExecutionPanel } from '@/components/execution/tour-execution-panel'
import { ExecutionStopsLayer } from '@/components/maps/execution-stops-layer'
import { BaseMap } from '@/components/maps/base-map'
import { executionLegend } from '@/components/maps/legend-items'
import type { LonLat } from '@/components/maps/geo'
import { MapFocus } from '@/components/maps/map-focus'
import { TourRouteLayer } from '@/components/maps/route-layers'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useCurrentUser } from '@/hooks/use-auth'
import { useMyTour } from '@/hooks/use-my-tours'
import { formatDistanceKm } from '@/lib/evaluation-format'
import { METHOD_LABELS } from '@/lib/evaluation-labels'
import { crewRole } from '@/lib/execution-labels'
import { STATUS_LABELS } from '@/lib/tour-labels'

/** AGENT page: one assigned tour with its map, stops, start, outcomes and completion. */
export default function MyTourPage() {
  const { id } = useParams()
  const { data: user } = useCurrentUser()
  const tour = useMyTour(id)
  const [selectedStopId, setSelectedStopId] = useState<string | null>(null)
  const [focusTarget, setFocusTarget] = useState<LonLat | null>(null)

  const back = (
    <Link to="/my-tours" className="text-sm underline underline-offset-2">
      ← My tours
    </Link>
  )

  if (tour.isPending) {
    return (
      <div className="flex flex-col gap-4">
        {back}
        <Skeleton className="h-24 w-full" />
      </div>
    )
  }
  if (tour.isError) {
    return (
      <div className="flex flex-col gap-4">
        {back}
        <Alert variant="destructive">
          <AlertTitle>Could not load the tour</AlertTitle>
          <AlertDescription>{tour.error.message}</AlertDescription>
        </Alert>
      </div>
    )
  }

  const t = tour.data
  return (
    <div className="flex flex-col gap-4">
      {back}
      <Card>
        <CardHeader>
          <CardTitle className="flex flex-wrap items-center gap-2">
            Tour of {t.planningDate}
            <Badge variant="secondary">{STATUS_LABELS[t.status]}</Badge>
            {t.origin === 'SYNTHETIC' && <Badge variant="outline">Synthetic demo</Badge>}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-x-6 gap-y-1 text-sm sm:grid-cols-2 lg:grid-cols-3">
            <div>
              <dt className="text-muted-foreground">Depot</dt>
              <dd>{t.depot.name}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Vehicle</dt>
              <dd>{t.vehicle?.registration ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">My role</dt>
              <dd>{crewRole(t.driver?.id, user?.agentId)}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Driver</dt>
              <dd>{t.driver?.displayName ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Collectors</dt>
              <dd>{t.collectors.map((c) => c.displayName).join(', ') || '—'}</dd>
            </div>
            {t.route && (
              <div>
                <dt className="text-muted-foreground">Road route</dt>
                <dd>
                  {METHOD_LABELS[t.route.method]}, {formatDistanceKm(t.route.distanceM)}
                </dd>
              </div>
            )}
          </dl>
        </CardContent>
      </Card>
      <div className="grid gap-4 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <div className="flex flex-col gap-2">
          <BaseMap
            className="h-[480px]"
            fitTo={[t.depot, ...t.stops]}
            legend={executionLegend(t.route?.method)}
          >
            <ExecutionStopsLayer
              tour={t}
              selectedStopId={selectedStopId}
              onSelectStop={setSelectedStopId}
            />
            {t.route && <TourRouteLayer route={t.route} />}
            <MapFocus target={focusTarget} />
          </BaseMap>
          <p className="text-xs text-muted-foreground">
            Basket and depot locations: NYC Open Data (DSNY). Road route: OpenStreetMap
            (© OpenStreetMap contributors).
          </p>
        </div>
        <Card>
          <CardHeader>
            <CardTitle>Execution</CardTitle>
          </CardHeader>
          <CardContent>
            <TourExecutionPanel tour={t} />
          </CardContent>
        </Card>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Stops in visit order</CardTitle>
        </CardHeader>
        <CardContent>
          <ExecutionStopList
            tour={t}
            selectedStopId={selectedStopId}
            onSelect={(s) => {
              setSelectedStopId(s.id)
              setFocusTarget({ longitude: s.longitude, latitude: s.latitude })
            }}
          />
        </CardContent>
      </Card>
    </div>
  )
}
