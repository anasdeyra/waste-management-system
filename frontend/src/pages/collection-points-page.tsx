import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'

import { PointDetails } from '@/components/collection-points/point-details'
import { PointFiltersBar } from '@/components/collection-points/point-filters'
import { PointForm } from '@/components/collection-points/point-form'
import { PointList } from '@/components/collection-points/point-list'
import { BaseMap } from '@/components/maps/base-map'
import { TrafficLayer } from '@/components/maps/traffic-layer'
import { TrafficPanel } from '@/components/maps/traffic-panel'
import { useTrafficOverlay } from '@/hooks/use-traffic'
import { enumParam } from '@/lib/url-filters'
import type { LonLat } from '@/components/maps/geo'
import { MapFocus } from '@/components/maps/map-focus'
import { PickedLocationLayer } from '@/components/maps/picked-location-layer'
import { PointsMapLayer } from '@/components/maps/points-map-layer'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Skeleton } from '@/components/ui/skeleton'
import {
  useAllCategories,
  useCollectionPoints,
  useDepots,
  useSetPointStatus,
  type PointFilters,
} from '@/hooks/use-collection-points'
import type { CollectionPoint } from '@/types/api'

type Mode = 'view' | 'create' | 'edit'

/** Collection points: filters, list, map, details, form and demand editor share one selection. */
export default function CollectionPointsPage() {
  const [params] = useSearchParams()
  const [filters, setFilters] = useState<PointFilters>({
    status: enumParam(params.get('status'), ['ACTIVE', 'INACTIVE'] as const) ?? 'ACTIVE',
  })
  const [showTraffic, setShowTraffic] = useState(false)
  const traffic = useTrafficOverlay(showTraffic)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [mode, setMode] = useState<Mode>('view')
  const [pickOnMap, setPickOnMap] = useState(false)
  const [pickedLocation, setPickedLocation] = useState<LonLat | null>(null)
  const [focusTarget, setFocusTarget] = useState<LonLat | null>(null)

  const points = useCollectionPoints(filters)
  const depots = useDepots()
  const categories = useAllCategories()
  const setStatus = useSetPointStatus()

  const list = points.data ?? []
  const selected = list.find((p) => p.id === selectedId) ?? null
  const allCategories = categories.data ?? []

  const select = (p: CollectionPoint, fly: boolean) => {
    setSelectedId(p.id)
    if (fly) setFocusTarget({ longitude: p.longitude, latitude: p.latitude })
  }

  const closeForm = () => {
    setMode('view')
    setPickOnMap(false)
    setPickedLocation(null)
  }

  const toggleStatus = (p: CollectionPoint) => {
    const next = p.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    setStatus.mutate(
      { id: p.id, status: next },
      {
        onSuccess: () =>
          toast.success(
            `${p.name} ${next === 'ACTIVE' ? 'activated' : 'deactivated'}. Change the status filter to see it.`,
          ),
        onError: (e) => toast.error(e.message),
      },
    )
  }

  const onMapClick = (loc: LonLat) => {
    if (pickOnMap) setPickedLocation(loc)
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">
          Collection points{points.data ? ` (${list.length})` : ''}
        </h1>
        <Button
          disabled={mode === 'create'}
          onClick={() => {
            setPickedLocation(null)
            setMode('create')
          }}
        >
          <Plus />
          New point
        </Button>
      </div>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
        <div className="flex flex-col gap-3">
          <PointFiltersBar filters={filters} categories={allCategories} onChange={setFilters} />
          {points.isPending && (
            <div className="flex flex-col gap-2">
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
              <Skeleton className="h-14 w-full" />
            </div>
          )}
          {points.isError && (
            <Alert variant="destructive">
              <AlertTitle>Could not load collection points</AlertTitle>
              <AlertDescription>{points.error.message}</AlertDescription>
            </Alert>
          )}
          {points.data && list.length === 0 && (
            <Empty>
              <EmptyHeader>
                <EmptyTitle>No collection points match these filters</EmptyTitle>
              </EmptyHeader>
            </Empty>
          )}
          {list.length > 0 && (
            <PointList points={list} selectedId={selectedId} onSelect={(p) => select(p, true)} />
          )}
        </div>
        <div className="flex flex-col gap-2">
          {depots.isError && (
            <Alert variant="destructive">
              <AlertTitle>Could not load depots</AlertTitle>
              <AlertDescription>{depots.error.message}</AlertDescription>
            </Alert>
          )}
          <BaseMap onMapClick={onMapClick}>
            {showTraffic && traffic.data?.available && <TrafficLayer links={traffic.data.links} />}
            <PointsMapLayer
              points={list}
              depots={depots.data ?? []}
              selectedId={selectedId}
              onSelect={(id) => setSelectedId(id)}
            />
            <MapFocus target={focusTarget} />
            {mode !== 'view' && <PickedLocationLayer location={pickedLocation} />}
          </BaseMap>
          <TrafficPanel
            enabled={showTraffic}
            onToggle={setShowTraffic}
            loading={traffic.isPending}
            overlay={traffic.data}
            error={traffic.error}
          />
          <p className="text-xs text-muted-foreground">
            Basket and depot locations: NYC Open Data (DSNY). Demand values marked Synthetic are
            demonstration data.
          </p>
        </div>
      </div>

      {mode === 'create' && (
        <PointForm
          key="create"
          point={null}
          categories={allCategories}
          pickOnMap={pickOnMap}
          pickedLocation={pickedLocation}
          onTogglePick={() => setPickOnMap((v) => !v)}
          onDone={(saved) => {
            closeForm()
            if (saved) {
              setSelectedId(saved.id)
              setFocusTarget({ longitude: saved.longitude, latitude: saved.latitude })
            }
          }}
        />
      )}
      {mode === 'edit' && selected && (
        <PointForm
          key={`edit-${selected.id}`}
          point={selected}
          categories={allCategories}
          pickOnMap={pickOnMap}
          pickedLocation={pickedLocation}
          onTogglePick={() => setPickOnMap((v) => !v)}
          onDone={(saved) => {
            closeForm()
            if (saved) setFocusTarget({ longitude: saved.longitude, latitude: saved.latitude })
          }}
        />
      )}
      {mode === 'view' && selected && (
        <PointDetails
          point={selected}
          categories={allCategories}
          busy={setStatus.isPending}
          onEdit={() => {
            setPickedLocation(null)
            setMode('edit')
          }}
          onToggleStatus={() => toggleStatus(selected)}
        />
      )}
    </div>
  )
}
