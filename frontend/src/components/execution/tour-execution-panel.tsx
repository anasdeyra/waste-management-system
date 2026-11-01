import { useState } from 'react'
import { toast } from 'sonner'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { useCompleteTour, useStartTour } from '@/hooks/use-my-tours'
import { ApiRequestError } from '@/lib/api-client'
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

function local(iso: string): string {
  return new Date(iso).toLocaleString()
}

/** Lifecycle actions of the assigned crew: start, progress, complete (US-27, US-29). */
export function TourExecutionPanel({ tour }: { tour: Tour }) {
  const start = useStartTour()
  const complete = useCompleteTour()
  const [refused, setRefused] = useState<TourIssue[] | null>(null)

  const onError = (e: Error) => {
    if (e instanceof ApiRequestError && e.status === 422 && e.body?.issues) {
      setRefused(e.body.issues)
    } else {
      toast.error(e.message)
    }
  }
  const resolved = tour.stops.filter((s) => s.status !== 'PENDING').length
  const pending = tour.stops.length - resolved
  const shownIssues = refused ?? (tour.startIssues.length > 0 ? tour.startIssues : null)

  return (
    <div className="flex flex-col gap-3">
      {tour.startedBy && tour.startedAt && (
        <p className="text-sm">
          Started by {tour.startedBy} at {local(tour.startedAt)}.
        </p>
      )}
      {tour.status === 'PLANNED' && (
        <>
          {shownIssues && (
            <Alert variant="destructive">
              <AlertTitle>Start blocked</AlertTitle>
              <AlertDescription>
                <IssueList issues={shownIssues} />
              </AlertDescription>
            </Alert>
          )}
          <div>
            <Button
              disabled={!tour.readyToStart || start.isPending}
              onClick={() =>
                start.mutate(
                  { id: tour.id },
                  {
                    onSuccess: () => {
                      setRefused(null)
                      toast.success('Tour started. Record the outcome of each stop.')
                    },
                    onError,
                  },
                )
              }
            >
              Start tour
            </Button>
          </div>
        </>
      )}
      {tour.status === 'IN_PROGRESS' && (
        <>
          <p className="text-sm">
            {resolved} of {tour.stops.length} stops resolved
          </p>
          <div>
            <Button
              disabled={!tour.readyToComplete || complete.isPending}
              onClick={() =>
                complete.mutate(
                  { id: tour.id },
                  {
                    onSuccess: () => {
                      setRefused(null)
                      toast.success('Tour completed.')
                    },
                    onError,
                  },
                )
              }
            >
              Complete tour
            </Button>
          </div>
          {pending > 0 && (
            <p className="text-sm text-muted-foreground">
              Complete tour is available when no stop is pending.
            </p>
          )}
          {refused && (
            <Alert variant="destructive">
              <AlertTitle>The server refused the completion</AlertTitle>
              <AlertDescription>
                <IssueList issues={refused} />
              </AlertDescription>
            </Alert>
          )}
        </>
      )}
      {tour.status === 'COMPLETED' && (
        <Alert>
          <AlertTitle>Tour completed</AlertTitle>
          <AlertDescription>
            {tour.completedBy && tour.completedAt && (
              <p>
                Completed by {tour.completedBy} at {local(tour.completedAt)}.
              </p>
            )}
            <p>Outcomes are locked. Corrections are made by a planner.</p>
          </AlertDescription>
        </Alert>
      )}
    </div>
  )
}
