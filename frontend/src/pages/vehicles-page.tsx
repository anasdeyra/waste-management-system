import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'

import { VehicleFormDialog } from '@/components/fleet/vehicle-form-dialog'
import { originLabel } from '@/components/maps/geo'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Empty, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useSetVehicleStatus, useVehicles } from '@/hooks/use-vehicles'
import { enumParam } from '@/lib/url-filters'
import type { ResourceStatus, Vehicle } from '@/types/api'

const ALL = 'ALL'

/** Vehicles: list, filter by availability, create, edit and toggle availability. */
export default function VehiclesPage() {
  const [params] = useSearchParams()
  const [filter, setFilter] = useState<ResourceStatus | undefined>(() =>
    enumParam(params.get('status'), ['AVAILABLE', 'UNAVAILABLE'] as const),
  )
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Vehicle | undefined>(undefined)
  const vehicles = useVehicles(filter)
  const setStatus = useSetVehicleStatus()

  const openDialog = (v?: Vehicle) => {
    setEditing(v)
    setDialogOpen(true)
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">Vehicles ({vehicles.data?.length ?? 0})</h1>
        <div className="flex items-end gap-3">
          <div className="flex flex-col gap-1">
            <Label htmlFor="vehicle-filter">Availability</Label>
            <Select
              value={filter ?? ALL}
              onValueChange={(v) => setFilter(v === ALL ? undefined : (v as ResourceStatus))}
            >
              <SelectTrigger id="vehicle-filter" className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>All</SelectItem>
                <SelectItem value="AVAILABLE">Available</SelectItem>
                <SelectItem value="UNAVAILABLE">Unavailable</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button onClick={() => openDialog()}>
            <Plus />
            New vehicle
          </Button>
        </div>
      </div>

      {vehicles.isPending && (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      )}
      {vehicles.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load vehicles</AlertTitle>
          <AlertDescription>{vehicles.error.message}</AlertDescription>
        </Alert>
      )}
      {vehicles.data && vehicles.data.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>No vehicles match this filter</EmptyTitle>
          </EmptyHeader>
        </Empty>
      )}
      {vehicles.data && vehicles.data.length > 0 && (
        <div className="rounded-lg border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Registration</TableHead>
                <TableHead>Capacity (kg)</TableHead>
                <TableHead>Origin</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Available</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {vehicles.data.map((v) => (
                <TableRow key={v.id}>
                  <TableCell className="font-medium">{v.registration}</TableCell>
                  <TableCell>{v.capacityKg}</TableCell>
                  <TableCell>{originLabel(v.origin)}</TableCell>
                  <TableCell>
                    <Badge variant={v.status === 'AVAILABLE' ? 'default' : 'secondary'}>
                      {v.status === 'AVAILABLE' ? 'Available' : 'Unavailable'}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Switch
                      aria-label={`Available: ${v.registration}`}
                      checked={v.status === 'AVAILABLE'}
                      disabled={setStatus.isPending}
                      onCheckedChange={(checked) =>
                        setStatus.mutate(
                          { id: v.id, status: checked ? 'AVAILABLE' : 'UNAVAILABLE' },
                          { onError: (err) => toast.error(err.message) },
                        )
                      }
                    />
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="outline" size="sm" onClick={() => openDialog(v)}>
                      Edit
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <VehicleFormDialog open={dialogOpen} vehicle={editing} onOpenChange={setDialogOpen} />
    </div>
  )
}
