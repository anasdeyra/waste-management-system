import { useState } from 'react'
import { toast } from 'sonner'
import { Link, useParams } from 'react-router-dom'

import { BaseMap } from '@/components/maps/base-map'
import type { LonLat } from '@/components/maps/geo'
import { MapFocus } from '@/components/maps/map-focus'
import { TourRouteLayer } from '@/components/maps/route-layers'
import { INPUT_ORDER_LABEL, TourStopsLayer } from '@/components/maps/tour-stops-layer'
import { CapacityIndicator } from '@/components/tours/capacity-indicator'
import { EvaluationPanel } from '@/components/tours/evaluation-panel'
import { TourCancelDialog } from '@/components/tours/tour-cancel-dialog'
import { TourConfirmPanel } from '@/components/tours/tour-confirm-panel'
import { TourAssignmentPanel } from '@/components/tours/tour-assignment-panel'
import { TourCandidateList } from '@/components/tours/tour-candidate-list'
import { TourHeaderForm } from '@/components/tours/tour-header-form'
import { TourIssues } from '@/components/tours/tour-issues'
import { TourStopList } from '@/components/tours/tour-stop-list'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useCollectionPoints, useDepots } from '@/hooks/use-collection-points'
import { useAgents } from '@/hooks/use-agents'
import {
  useAddTourStop,
  useRemoveTourStop,
  useReorderTourStops,
  useTour,
} from '@/hooks/use-tours'
import { useVehicles } from '@/hooks/use-vehicles'
import { METHOD_LABELS } from '@/lib/evaluation-labels'
import { STOP_STATUS_LEGEND } from '@/lib/execution-labels'
import { STATUS_LABELS } from '@/lib/tour-labels'

/** Tour planner: map and list stop selection, input order, crew, capacity and validation. */
export default function TourPlannerPage() {
  const { id } = useParams()
  const tour = useTour(id)
  const candidates = useCollectionPoints({ eligible: true })
  const depots = useDepots()
  const vehicles = useVehicles()
  const agents = useAgents()
  const addStop = useAddTourStop()
  const removeStop = useRemoveTourStop()
  const reorder = useReorderTourStops()
  const [selectedStopId, setSelectedStopId] = useState<string | null>(null)
  const [focusTarget, setFocusTarget] = useState<LonLat | null>(null)

  const back = (
    <Link to="/tours" className="text-sm underline underline-offset-2">
      ← Tours
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
  const editable = t.status === 'DRAFT'
  const busy = addStop.isPending || removeStop.isPending || reorder.isPending
  const inTour = new Set(t.stops.map((s) => s.collectionPointId))
  const onError = (e: Error) => toast.error(e.message)

  const doAdd = (pointId: string) =>
    addStop.mutate({ id: t.id, body: { collectionPointId: pointId } }, { onError })
  const doRemove = (stopId: string) => removeStop.mutate({ id: t.id, stopId }, { onError })
  const doReorder = (stopIds: string[]) =>
    reorder.mutate({ id: t.id, body: { stopIds } }, { onError })

  return (
    <div className="flex flex-col gap-4">
      {back}
      <TourHeaderForm
        key={`${t.id}-${t.revision}`}
        tour={t}
        depots={depots.data ?? []}
        editable={editable}
      />
      <TourIssues tour={t} />
      {!editable && (
        <Alert>
          <AlertTitle>Stops, order and crew are locked</AlertTitle>
          <AlertDescription>
            This tour is {STATUS_LABELS[t.status]}. Stops, order and crew are locked.
          </AlertDescription>
        </Alert>
      )}
      <div className="grid gap-4 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <div className="flex flex-col gap-2">
          <BaseMap className="h-[520px]">
            <TourStopsLayer
              tour={t}
              candidates={candidates.data ?? []}
              selectedStopId={selectedStopId}
              editable={editable}
              onAdd={doAdd}
              onRemove={doRemove}
              onSelectStop={setSelectedStopId}
            />
            {t.route && <TourRouteLayer route={t.route} />}
            <MapFocus target={focusTarget} />
          </BaseMap>
          <p className="text-xs text-muted-foreground">
            Numbers show the current stop order. Dashed lines connect the stops in that order; they
            are straight lines, not a route ({INPUT_ORDER_LABEL}). Compare the road routes in Route
            evaluation below.
            {t.route &&
              ` The solid line is the attached road route (${METHOD_LABELS[t.route.method]}) on road graph ${t.route.roadGraphVersion}.`}
          </p>
          {!editable && <p className="text-xs text-muted-foreground">{STOP_STATUS_LEGEND}</p>}
          <p className="text-xs text-muted-foreground">
            Basket and depot locations: NYC Open Data (DSNY). Demand values marked Synthetic are
            demonstration data.
          </p>
        </div>
        <div className="flex flex-col gap-4">
          <Card>
            <CardHeader>
              <CardTitle>Confirmation</CardTitle>
            </CardHeader>
            <CardContent>
              <TourConfirmPanel tour={t} />
            </CardContent>
          </Card>
          {t.cancellable && (
            <Card>
              <CardHeader>
                <CardTitle>Cancellation</CardTitle>
              </CardHeader>
              <CardContent>
                <TourCancelDialog tour={t} />
              </CardContent>
            </Card>
          )}
          {t.status !== 'DRAFT' && (
            <Card>
              <CardHeader>
                <CardTitle>Execution</CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col gap-2 text-sm">
                <p>Status: {STATUS_LABELS[t.status]}</p>
                {t.startedBy && t.startedAt && (
                  <p>
                    Started by {t.startedBy} at {new Date(t.startedAt).toLocaleString()}
                  </p>
                )}
                <p>
                  {t.stops.filter((s) => s.status !== 'PENDING').length} of {t.stops.length} stops
                  resolved
                </p>
                {t.completedBy && t.completedAt && (
                  <p>
                    Completed by {t.completedBy} at {new Date(t.completedAt).toLocaleString()}
                  </p>
                )}
                {t.status === 'PLANNED' && t.startIssues.length > 0 && (
                  <Alert variant="destructive">
                    <AlertTitle>Start blocked</AlertTitle>
                    <AlertDescription>
                      <ul className="list-disc pl-4">
                        {t.startIssues.map((i, idx) => (
                          <li key={`${i.code}-${idx}`}>{i.message}</li>
                        ))}
                      </ul>
                    </AlertDescription>
                  </Alert>
                )}
                {t.status === 'CANCELLED' && t.cancelledBy && t.cancelledAt && (
                  <p>
                    Cancelled by {t.cancelledBy} at {new Date(t.cancelledAt).toLocaleString()} (was{' '}
                    {t.cancelledFromStatus ? STATUS_LABELS[t.cancelledFromStatus] : 'unknown'})
                  </p>
                )}
                {t.status === 'CANCELLED' && t.cancellationReason && (
                  <p>Reason: {t.cancellationReason}</p>
                )}
                <p className="text-muted-foreground">Execution updates are made by the assigned crew.</p>
                {t.correctable && (
                  <p className="text-muted-foreground">
                    Planners can correct recorded outcomes; every correction is kept and audited.
                  </p>
                )}
              </CardContent>
            </Card>
          )}
          <Card>
            <CardHeader>
              <CardTitle>Capacity</CardTitle>
            </CardHeader>
            <CardContent>
              <CapacityIndicator capacity={t.capacity} />
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Assignment</CardTitle>
            </CardHeader>
            <CardContent>
              <TourAssignmentPanel
                key={`${t.id}-${t.revision}`}
                tour={t}
                vehicles={vehicles.data ?? []}
                agents={agents.data ?? []}
                editable={editable}
              />
            </CardContent>
          </Card>
        </div>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Route evaluation</CardTitle>
        </CardHeader>
        <CardContent>
          <EvaluationPanel tour={t} />
        </CardContent>
      </Card>
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Stops in visit order ({t.stops.length})</CardTitle>
          </CardHeader>
          <CardContent>
            <TourStopList
              tour={t}
              selectedStopId={selectedStopId}
              editable={editable}
              busy={busy}
              onSelect={(s) => {
                setSelectedStopId(s.id)
                setFocusTarget({ longitude: s.longitude, latitude: s.latitude })
              }}
              onReorder={doReorder}
              onRemove={doRemove}
            />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Add stops</CardTitle>
          </CardHeader>
          <CardContent>
            <TourCandidateList
              candidates={candidates.data ?? []}
              inTour={inTour}
              editable={editable}
              busy={busy}
              onAdd={doAdd}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
