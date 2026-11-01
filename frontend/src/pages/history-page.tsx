import { useState } from 'react'
import { Link } from 'react-router-dom'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Empty, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useAgents } from '@/hooks/use-agents'
import { useCollectionPoints } from '@/hooks/use-collection-points'
import { useTourHistory } from '@/hooks/use-tours'
import type { HistoryFilters } from '@/hooks/use-tours'
import { useVehicles } from '@/hooks/use-vehicles'
import { STATUS_LABELS } from '@/lib/tour-labels'

const ALL = 'ALL'

function local(iso?: string): string {
  return iso ? new Date(iso).toLocaleString() : '—'
}

/** Operational history: completed and cancelled tours, read-only (US-32). */
export default function HistoryPage() {
  const [filters, setFilters] = useState<HistoryFilters>({})
  const history = useTourHistory(filters)
  const vehicles = useVehicles()
  const agents = useAgents()
  const points = useCollectionPoints({})
  const set = (patch: Partial<HistoryFilters>) => setFilters((f) => ({ ...f, ...patch }))
  const sortedPoints = [...(points.data ?? [])].sort((a, b) => a.name.localeCompare(b.name))
  const choose = (value: string) => (value === ALL ? undefined : value)

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-xl font-semibold">Operational history</h1>
        <p className="text-sm text-muted-foreground">
          Completed and cancelled tours. Historical tours are read-only; they cannot be reopened or deleted.
        </p>
      </div>
      <div className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1">
          <Label htmlFor="history-from">From</Label>
          <Input
            id="history-from"
            type="date"
            className="w-44"
            value={filters.from ?? ''}
            onChange={(e) => set({ from: e.target.value || undefined })}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="history-to">To</Label>
          <Input
            id="history-to"
            type="date"
            className="w-44"
            value={filters.to ?? ''}
            onChange={(e) => set({ to: e.target.value || undefined })}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="history-status">Status</Label>
          <Select
            value={filters.status ?? ALL}
            onValueChange={(v) => set({ status: v === ALL ? undefined : (v as 'COMPLETED' | 'CANCELLED') })}
          >
            <SelectTrigger id="history-status" className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All</SelectItem>
              <SelectItem value="COMPLETED">Completed</SelectItem>
              <SelectItem value="CANCELLED">Cancelled</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="history-vehicle">Vehicle</Label>
          <Select
            value={filters.vehicleId ?? ALL}
            onValueChange={(v) => set({ vehicleId: choose(v) })}
          >
            <SelectTrigger id="history-vehicle" className="w-44">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All vehicles</SelectItem>
              {(vehicles.data ?? []).map((v) => (
                <SelectItem key={v.id} value={v.id}>
                  {v.registration}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="history-agent">Agent</Label>
          <Select value={filters.agentId ?? ALL} onValueChange={(v) => set({ agentId: choose(v) })}>
            <SelectTrigger id="history-agent" className="w-44">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All agents</SelectItem>
              {(agents.data ?? []).map((a) => (
                <SelectItem key={a.id} value={a.id}>
                  {a.displayName}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="history-point">Collection point</Label>
          <Select value={filters.pointId ?? ALL} onValueChange={(v) => set({ pointId: choose(v) })}>
            <SelectTrigger id="history-point" className="w-56">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All points</SelectItem>
              {sortedPoints.map((p) => (
                <SelectItem key={p.id} value={p.id}>
                  {p.sourceIdentifier ? `${p.name} (${p.sourceIdentifier})` : p.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <Button variant="outline" onClick={() => setFilters({})}>
          Clear filters
        </Button>
      </div>

      {history.isPending && <Skeleton className="h-10 w-full" />}
      {history.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load the history</AlertTitle>
          <AlertDescription>{history.error.message}</AlertDescription>
        </Alert>
      )}
      {history.data && history.data.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>No completed or cancelled tours match these filters.</EmptyTitle>
          </EmptyHeader>
        </Empty>
      )}
      {history.data && history.data.length > 0 && (
        <div className="rounded-lg border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Vehicle</TableHead>
                <TableHead>Driver</TableHead>
                <TableHead>Stops</TableHead>
                <TableHead>Corrections</TableHead>
                <TableHead>Ended</TableHead>
                <TableHead>Reason</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {history.data.map((t) => (
                <TableRow key={t.id}>
                  <TableCell className="font-medium">{t.planningDate}</TableCell>
                  <TableCell>
                    <Badge variant={t.status === 'CANCELLED' ? 'destructive' : 'secondary'}>
                      {STATUS_LABELS[t.status]}
                    </Badge>
                  </TableCell>
                  <TableCell>{t.vehicleRegistration ?? '—'}</TableCell>
                  <TableCell>{t.driverName ?? '—'}</TableCell>
                  <TableCell>
                    {t.terminalStopCount}/{t.stopCount} resolved
                  </TableCell>
                  <TableCell>{t.correctionCount}</TableCell>
                  <TableCell>{local(t.completedAt ?? t.cancelledAt)}</TableCell>
                  <TableCell>{t.cancellationReason ?? '—'}</TableCell>
                  <TableCell>
                    <Link to={`/tours/${t.id}`} className="underline underline-offset-2">
                      Open
                    </Link>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
