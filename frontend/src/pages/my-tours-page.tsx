import { useState } from 'react'
import { Link } from 'react-router-dom'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
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
import { useCurrentUser } from '@/hooks/use-auth'
import { useMyTours } from '@/hooks/use-my-tours'
import { crewRole } from '@/lib/execution-labels'
import { STATUS_LABELS } from '@/lib/tour-labels'
import type { TourStatus } from '@/types/api'

const ALL = 'ALL'
const FILTERS: TourStatus[] = ['PLANNED', 'IN_PROGRESS', 'COMPLETED']

/** AGENT page: the tours whose crew includes the signed-in agent (US-26). */
export default function MyToursPage() {
  const { data: user } = useCurrentUser()
  const [status, setStatus] = useState<TourStatus | undefined>(undefined)
  const tours = useMyTours(status)

  if (!user?.agentId) {
    return (
      <div className="flex flex-col gap-4">
        <h1 className="text-xl font-semibold">My tours</h1>
        <Alert>
          <AlertTitle>Your account is not linked to an operational agent</AlertTitle>
          <AlertDescription>
            Ask an administrator to link your account to your agent record.
          </AlertDescription>
        </Alert>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">My tours</h1>
        <div className="flex flex-col gap-1">
          <Label htmlFor="my-tours-status">Status</Label>
          <Select
            value={status ?? ALL}
            onValueChange={(v) => setStatus(v === ALL ? undefined : (v as TourStatus))}
          >
            <SelectTrigger id="my-tours-status" className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All</SelectItem>
              {FILTERS.map((s) => (
                <SelectItem key={s} value={s}>
                  {STATUS_LABELS[s]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>
      {tours.isPending && <Skeleton className="h-24 w-full" />}
      {tours.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load your tours</AlertTitle>
          <AlertDescription>{tours.error.message}</AlertDescription>
        </Alert>
      )}
      {tours.data && tours.data.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>No assigned tours</EmptyTitle>
            <EmptyDescription>Planned tours appear here once a planner confirms them.</EmptyDescription>
          </EmptyHeader>
        </Empty>
      )}
      {tours.data && tours.data.length > 0 && (
        <div className="overflow-x-auto rounded-lg border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Planning date</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Depot</TableHead>
                <TableHead>Stops resolved</TableHead>
                <TableHead>My role</TableHead>
                <TableHead>Vehicle</TableHead>
                <TableHead>
                  <span className="sr-only">Actions</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {tours.data.map((t) => (
                <TableRow key={t.id}>
                  <TableCell className="font-medium">{t.planningDate}</TableCell>
                  <TableCell>
                    <Badge variant="secondary">{STATUS_LABELS[t.status]}</Badge>
                    {t.origin === 'SYNTHETIC' && (
                      <Badge variant="outline" className="ml-2">
                        Synthetic demo
                      </Badge>
                    )}
                  </TableCell>
                  <TableCell>{t.depotName}</TableCell>
                  <TableCell>
                    {t.terminalStopCount} of {t.stopCount} stops
                  </TableCell>
                  <TableCell>{crewRole(t.driverId, user.agentId)}</TableCell>
                  <TableCell>{t.vehicleRegistration ?? '—'}</TableCell>
                  <TableCell>
                    <Link to={`/my-tours/${t.id}`} className="underline underline-offset-2">
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
