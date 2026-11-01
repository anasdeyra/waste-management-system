import { useState } from 'react'
import { toast } from 'sonner'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { STATUS_LABELS } from '@/lib/tour-labels'
import { useUpdateTour } from '@/hooks/use-tours'
import type { Depot, Tour } from '@/types/api'

/** Planning date and depot of the draft; saved together. Remount (key) after each save. */
export function TourHeaderForm({
  tour,
  depots,
  editable,
}: {
  tour: Tour
  depots: Depot[]
  editable: boolean
}) {
  const [date, setDate] = useState(tour.planningDate)
  const [depotId, setDepotId] = useState(tour.depot.id)
  const update = useUpdateTour()
  const changed = date !== tour.planningDate || depotId !== tour.depot.id

  const save = () =>
    update.mutate(
      { id: tour.id, body: { planningDate: date, depotId } },
      {
        onSuccess: () => toast.success('Tour saved'),
        onError: (e) => toast.error(e.message),
      },
    )

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex flex-wrap items-center gap-2">
          Tour {tour.planningDate}
          <Badge variant="secondary">{STATUS_LABELS[tour.status]}</Badge>
          <span className="text-xs font-normal text-muted-foreground">
            revision {tour.revision} · updated {new Date(tour.updatedAt).toLocaleString()}
          </span>
        </CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {!editable && (
          <p className="text-sm text-muted-foreground">Only DRAFT tours can be edited.</p>
        )}
        <div className="flex flex-wrap items-end gap-3">
          <div className="flex flex-col gap-1">
            <Label htmlFor="header-date">Planning date</Label>
            <Input
              id="header-date"
              type="date"
              value={date}
              disabled={!editable}
              onChange={(e) => setDate(e.target.value)}
              className="w-44"
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="header-depot">Depot</Label>
            <Select value={depotId} onValueChange={setDepotId} disabled={!editable}>
              <SelectTrigger id="header-depot" className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {depots.map((d) => (
                  <SelectItem key={d.id} value={d.id}>
                    {d.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <Button onClick={save} disabled={!editable || !changed || update.isPending}>
            Save
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
