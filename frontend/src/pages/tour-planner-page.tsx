import { useState } from 'react'
import { toast } from 'sonner'
import { Link, useParams } from 'react-router-dom'

import { BaseMap } from '@/components/maps/base-map'
import type { LonLat } from '@/components/maps/geo'
import { MapFocus } from '@/components/maps/map-focus'
import { INPUT_ORDER_LABEL, TourStopsLayer } from '@/components/maps/tour-stops-layer'
import { CapacityIndicator } from '@/components/tours/capacity-indicator'
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
            <MapFocus target={focusTarget} />
          </BaseMap>
          <p className="text-xs text-muted-foreground">
            Numbers show the input order. Dashed lines connect stops in input order; they are
            straight lines, not a route. Road routes arrive with routing (S5). Layer: {INPUT_ORDER_LABEL}.
          </p>
          <p className="text-xs text-muted-foreground">
            Basket and depot locations: NYC Open Data (DSNY). Demand values marked Synthetic are
            demonstration data.
          </p>
        </div>
        <div className="flex flex-col gap-4">
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
      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Stops in input order ({t.stops.length})</CardTitle>
          </CardHeader>
          <CardContent>
            <TourStopList
              stops={t.stops}
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
