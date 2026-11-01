import { useState } from 'react'
import { toast } from 'sonner'

import { EvaluationComparison } from '@/components/tours/evaluation-comparison'
import { EvaluationRunSummary } from '@/components/tours/evaluation-run-summary'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { useEvaluation, useRunEvaluation, useTourEvaluations } from '@/hooks/use-evaluations'
import { ApiRequestError } from '@/lib/api-client'
import { formatInstant } from '@/lib/evaluation-format'
import { DECISION_LABELS } from '@/lib/evaluation-labels'
import type { Tour } from '@/types/api'

/** "Route evaluation" card body: run the evaluation, pick a retained run, compare its methods. */
export function EvaluationPanel({ tour }: { tour: Tour }) {
  const runs = useTourEvaluations(tour.id)
  const evaluate = useRunEvaluation()
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null)
  const effectiveId = selectedRunId ?? runs.data?.[0]?.id
  const run = useEvaluation(effectiveId)

  const startEvaluation = () => {
    evaluate.reset()
    evaluate.mutate(
      { tourId: tour.id },
      {
        onSuccess: (r) => setSelectedRunId(r.id),
        onError: (e) => {
          if (!(e instanceof ApiRequestError && (e.status === 422 || e.status === 503))) {
            toast.error(e.message)
          }
        },
      },
    )
  }

  const err = evaluate.error
  const evalError =
    err instanceof ApiRequestError && (err.status === 422 || err.status === 503) ? err : null

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3">
        {tour.status === 'DRAFT' && (
          <Button
            onClick={startEvaluation}
            disabled={!tour.structurallyValid || evaluate.isPending}
          >
            {evaluate.isPending ? 'Routing and optimizing…' : 'Evaluate routes'}
          </Button>
        )}
        {tour.status === 'DRAFT' && !tour.structurallyValid && (
          <span className="text-sm text-muted-foreground">
            Resolve the draft issues above before evaluating.
          </span>
        )}
        {!!runs.data?.length && effectiveId && (
          <div className="flex items-center gap-2 text-sm">
            <span id="evaluation-run-label">Evaluation run</span>
            <Select value={effectiveId} onValueChange={setSelectedRunId}>
              <SelectTrigger aria-labelledby="evaluation-run-label" className="w-96">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {runs.data.map((r) => (
                  <SelectItem key={r.id} value={r.id}>
                    {`${formatInstant(r.createdAt)} · ${r.status}${r.decision ? ' · ' + DECISION_LABELS[r.decision] : ''}`}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}
      </div>
      {evalError && (
        <Alert variant="destructive">
          <AlertTitle>
            {evalError.status === 503 ? 'Road routing unavailable' : 'Evaluation not possible'}
          </AlertTitle>
          <AlertDescription>{evalError.message}</AlertDescription>
        </Alert>
      )}
      {runs.isPending && <Skeleton className="h-24 w-full" />}
      {runs.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load the evaluations</AlertTitle>
          <AlertDescription>{runs.error.message}</AlertDescription>
        </Alert>
      )}
      {runs.data && runs.data.length === 0 && (
        <p className="text-sm text-muted-foreground">
          No evaluation yet. Evaluate the routes to compare the input order, nearest neighbor and NN +
          2-opt on the road network.
        </p>
      )}
      {effectiveId && run.isPending && <Skeleton className="h-64 w-full" />}
      {run.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load the evaluation run</AlertTitle>
          <AlertDescription>{run.error.message}</AlertDescription>
        </Alert>
      )}
      {run.data && (
        <>
          <EvaluationRunSummary run={run.data} tour={tour} />
          {run.data.status === 'COMPLETED' && <EvaluationComparison key={run.data.id} run={run.data} />}
        </>
      )}
    </div>
  )
}
