import { useState } from 'react'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useAssignTour } from '@/hooks/use-tours'
import type { Agent, Tour, Vehicle } from '@/types/api'

const NONE = 'none'

/**
 * Vehicle, driver and up to two collectors. Local state starts from the tour; the parent
 * remounts the panel (key = revision) after every save. Client exclusions are hints only.
 */
export function TourAssignmentPanel({
  tour,
  vehicles,
  agents,
  editable,
}: {
  tour: Tour
  vehicles: Vehicle[]
  agents: Agent[]
  editable: boolean
}) {
  const [vehicleId, setVehicleId] = useState(tour.vehicle?.id ?? NONE)
  const [driverId, setDriverId] = useState(tour.driver?.id ?? NONE)
  const [c1, setC1] = useState(tour.collectors[0]?.id ?? NONE)
  const [c2, setC2] = useState(tour.collectors[1]?.id ?? NONE)
  const assign = useAssignTour()

  const onTourAgents = new Set([tour.driver?.id, ...tour.collectors.map((c) => c.id)])
  const agentOptions = (exclude: string[]) =>
    agents.filter(
      (a) => !exclude.includes(a.id) && (a.status === 'AVAILABLE' || onTourAgents.has(a.id)),
    )
  const vehicleOptions = vehicles.filter(
    (v) => v.status === 'AVAILABLE' || v.id === tour.vehicle?.id,
  )

  const save = () =>
    assign.mutate(
      {
        id: tour.id,
        body: {
          vehicleId: vehicleId === NONE ? undefined : vehicleId,
          driverId: driverId === NONE ? undefined : driverId,
          collectorIds: [c1, c2].filter((c) => c !== NONE),
        },
      },
      {
        onSuccess: () => toast.success('Assignment saved'),
        onError: (e) => toast.error(e.message),
      },
    )

  const agentSelect = (
    id: string,
    label: string,
    value: string,
    set: (v: string) => void,
    exclude: string[],
  ) => (
    <div className="flex flex-col gap-1">
      <Label htmlFor={id}>{label}</Label>
      <Select value={value} onValueChange={set} disabled={!editable}>
        <SelectTrigger id={id}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={NONE}>— none —</SelectItem>
          {agentOptions(exclude).map((a) => (
            <SelectItem key={a.id} value={a.id}>
              {a.displayName}
              {a.status === 'UNAVAILABLE' ? ' (unavailable)' : ''}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-1">
        <Label htmlFor="assign-vehicle">Vehicle</Label>
        <Select value={vehicleId} onValueChange={setVehicleId} disabled={!editable}>
          <SelectTrigger id="assign-vehicle">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={NONE}>— none —</SelectItem>
            {vehicleOptions.map((v) => (
              <SelectItem key={v.id} value={v.id}>
                {v.registration} · {v.capacityKg} kg
                {v.status === 'UNAVAILABLE' ? ' (unavailable)' : ''}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {agentSelect(
        'assign-driver',
        'Driver',
        driverId,
        setDriverId,
        [c1, c2].filter((c) => c !== NONE),
      )}
      {agentSelect(
        'assign-collector-1',
        'Collector 1',
        c1,
        setC1,
        [driverId, c2].filter((c) => c !== NONE),
      )}
      {agentSelect(
        'assign-collector-2',
        'Collector 2',
        c2,
        setC2,
        [driverId, c1].filter((c) => c !== NONE),
      )}
      <Button onClick={save} disabled={!editable || assign.isPending}>
        Save assignment
      </Button>
    </div>
  )
}
