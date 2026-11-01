import { Link, useParams } from 'react-router-dom'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Skeleton } from '@/components/ui/skeleton'
import { useTour } from '@/hooks/use-tours'

/** Tour planner (completed in T5). */
export default function TourPlannerPage() {
  const { id } = useParams()
  const tour = useTour(id)
  return (
    <div className="flex flex-col gap-4">
      <Link to="/tours" className="text-sm underline underline-offset-2">
        ← Tours
      </Link>
      {tour.isPending && <Skeleton className="h-10 w-full" />}
      {tour.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load the tour</AlertTitle>
          <AlertDescription>{tour.error.message}</AlertDescription>
        </Alert>
      )}
      {tour.data && (
        <h1 className="text-xl font-semibold">
          Tour {tour.data.planningDate} · {tour.data.depot.name}{' '}
          <Badge variant="secondary">{tour.data.status}</Badge>
        </h1>
      )}
    </div>
  )
}
