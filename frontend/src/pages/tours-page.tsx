import { Plus } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'

import { TourCreateDialog } from '@/components/tours/tour-create-dialog'
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
import { METHOD_LABELS } from '@/lib/evaluation-labels'
import { STATUS_LABELS } from '@/lib/tour-labels'
import { useTours } from '@/hooks/use-tours'
import type { TourStatus } from '@/types/api'

const ALL = 'ALL'

/** Tours: filterable list of tour drafts and the entry point to create one. */
export default function ToursPage() {
  const [status, setStatus] = useState<TourStatus | undefined>(undefined)
  const [planningDate, setPlanningDate] = useState('')
  const [dialogOpen, setDialogOpen] = useState(false)
  const tours = useTours({ status, planningDate: planningDate || undefined })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">Tours ({tours.data?.length ?? 0})</h1>
        <div className="flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1">
            <Label htmlFor="tour-status-filter">Status</Label>
            <Select
              value={status ?? ALL}
              onValueChange={(v) => setStatus(v === ALL ? undefined : (v as TourStatus))}
            >
              <SelectTrigger id="tour-status-filter" className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>All</SelectItem>
                {(Object.keys(STATUS_LABELS) as TourStatus[]).map((s) => (
                  <SelectItem key={s} value={s}>
                    {STATUS_LABELS[s]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="tour-date-filter">Planning date</Label>
            <div className="flex gap-1">
              <Input
                id="tour-date-filter"
                type="date"
                value={planningDate}
                onChange={(e) => setPlanningDate(e.target.value)}
                className="w-44"
              />
              <Button variant="outline" onClick={() => setPlanningDate('')} disabled={!planningDate}>
                Clear
              </Button>
            </div>
          </div>
          <Button onClick={() => setDialogOpen(true)}>
            <Plus />
            New draft tour
          </Button>
        </div>
      </div>

      {tours.isPending && (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      )}
      {tours.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load tours</AlertTitle>
          <AlertDescription>{tours.error.message}</AlertDescription>
        </Alert>
      )}
      {tours.data && tours.data.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>No tours yet — create a draft</EmptyTitle>
          </EmptyHeader>
        </Empty>
      )}
      {tours.data && tours.data.length > 0 && (
        <div className="rounded-lg border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Planning date</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Depot</TableHead>
                <TableHead>Stops</TableHead>
                <TableHead>Planned demand</TableHead>
                <TableHead>Vehicle</TableHead>
                <TableHead>Driver</TableHead>
                <TableHead>Collectors</TableHead>
                <TableHead>Validation</TableHead>
                <TableHead>Route</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {tours.data.map((t) => (
                <TableRow key={t.id}>
                  <TableCell className="font-medium">
                    <Link to={`/tours/${t.id}`} className="underline underline-offset-2">
                      {t.planningDate}
                    </Link>
                  </TableCell>
                  <TableCell>
                    <Badge variant="secondary">{STATUS_LABELS[t.status]}</Badge>
                  </TableCell>
                  <TableCell>{t.depotName}</TableCell>
                  <TableCell>{t.stopCount}</TableCell>
                  <TableCell>{t.totalPlannedDemandKg} kg</TableCell>
                  <TableCell>{t.vehicleRegistration ?? '—'}</TableCell>
                  <TableCell>{t.driverName ?? '—'}</TableCell>
                  <TableCell>{t.collectorCount}</TableCell>
                  <TableCell>
                    <Badge variant={t.structurallyValid ? 'default' : 'destructive'}>
                      {t.structurallyValid
                        ? 'Ready'
                        : `${t.issueCount} issue${t.issueCount === 1 ? '' : 's'}`}
                    </Badge>
                  </TableCell>
                  <TableCell>{t.routeMethod ? METHOD_LABELS[t.routeMethod] : '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <TourCreateDialog open={dialogOpen} onOpenChange={setDialogOpen} />
    </div>
  )
}
