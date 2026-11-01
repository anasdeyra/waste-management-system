import { useState } from 'react'
import { toast } from 'sonner'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { useConfirmTour } from '@/hooks/use-tours'
import { ApiRequestError } from '@/lib/api-client'
import { formatDistanceKm, formatDuration } from '@/lib/evaluation-format'
import { METHOD_LABELS } from '@/lib/evaluation-labels'
import { STATUS_LABELS } from '@/lib/tour-labels'
import type { Tour, TourIssue } from '@/types/api'

function IssueList({ issues }: { issues: TourIssue[] }) {
  return (
    <ul className="list-disc pl-4">
      {issues.map((i, idx) => (
        <li key={`${i.code}-${idx}`}>{i.message}</li>
      ))}
    </ul>
  )
}

/** Confirmation gate (US-25): shows every blocking issue and confirms a ready draft. */
export function TourConfirmPanel({ tour }: { tour: Tour }) {
  const confirm = useConfirmTour()
  const [serverIssues, setServerIssues] = useState<TourIssue[] | null>(null)

  if (tour.status !== 'DRAFT') {
    const route = tour.route
    return (
      <div className="flex flex-col gap-3">
        <Alert>
          <AlertTitle>{STATUS_LABELS[tour.status]}</AlertTitle>
          <AlertDescription>
            Stops, order and crew are locked (US-25). Cancellation arrives with history in a later
            sprint.
          </AlertDescription>
        </Alert>
        {route && (
          <dl className="grid gap-x-6 gap-y-1 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted-foreground">Road route</dt>
              <dd>{METHOD_LABELS[route.method]}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Distance</dt>
              <dd>{formatDistanceKm(route.distanceM)}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Duration</dt>
              <dd>{formatDuration(route.durationS)}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Road graph</dt>
              <dd>{route.roadGraphVersion}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Evaluation run</dt>
              <dd>{route.evaluationRunId.slice(0, 8)}</dd>
            </div>
          </dl>
        )}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      {tour.readyToConfirm ? (
        <p className="text-sm">
          Ready to confirm. All confirmation checks pass (date, depot, stops, road route, capacity,
          vehicle, driver, collectors, availability).
        </p>
      ) : (
        <Alert variant="destructive">
          <AlertTitle>Confirmation blocked</AlertTitle>
          <AlertDescription>
            <IssueList issues={tour.confirmationIssues} />
          </AlertDescription>
        </Alert>
      )}
      <div>
        <Button
          disabled={!tour.readyToConfirm || confirm.isPending}
          onClick={() =>
            confirm.mutate(
              { id: tour.id },
              {
                onSuccess: () => {
                  setServerIssues(null)
                  toast.success('Tour confirmed as planned. Its stops, order and crew are now locked.')
                },
                onError: (e) => {
                  if (e instanceof ApiRequestError && e.status === 422 && e.body?.issues) {
                    setServerIssues(e.body.issues)
                  } else {
                    toast.error(e.message)
                  }
                },
              },
            )
          }
        >
          Confirm as planned
        </Button>
      </div>
      {serverIssues && (
        <Alert variant="destructive">
          <AlertTitle>The server refused the confirmation</AlertTitle>
          <AlertDescription>
            <IssueList issues={serverIssues} />
          </AlertDescription>
        </Alert>
      )}
    </div>
  )
}
