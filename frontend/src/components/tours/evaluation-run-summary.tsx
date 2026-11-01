import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { formatInstant } from '@/lib/evaluation-format'
import { DECISION_LABELS, METHOD_LABELS } from '@/lib/evaluation-labels'
import type { EvaluationRun, Tour } from '@/types/api'

/** Provenance, freshness, decision and failures of one retained evaluation run. */
export function EvaluationRunSummary({ run, tour }: { run: EvaluationRun; tour: Tour }) {
  const fresh = run.tourRevision === tour.revision
  const infeasible = run.results.filter((r) => !r.feasible)
  return (
    <div className="flex flex-col gap-3">
      <dl className="grid gap-x-6 gap-y-1 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <div>
          <dt className="text-muted-foreground">Created</dt>
          <dd>
            {formatInstant(run.createdAt)} by {run.createdBy}
          </dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Road graph</dt>
          <dd>{run.roadGraphVersion}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Routing engine</dt>
          <dd>{run.routingEngine}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Source snapshots</dt>
          <dd>{run.sourceSnapshotIds.join(', ')}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Stops</dt>
          <dd>{run.pointCount}</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Total demand</dt>
          <dd>{run.totalDemandKg} kg</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Vehicle capacity</dt>
          <dd>{run.vehicleCapacityKg} kg</dd>
        </div>
        <div>
          <dt className="text-muted-foreground">Service time</dt>
          <dd>{run.serviceTimeSeconds} s per stop</dd>
        </div>
      </dl>
      <div className="flex flex-wrap items-center gap-2 text-sm">
        {fresh ? (
          <Badge variant="secondary">Current draft</Badge>
        ) : (
          <>
            <Badge variant="destructive">Stale</Badge>
            <span>
              The draft changed since this run (revision {run.tourRevision}, now {tour.revision}).
              Evaluate again before deciding.
            </span>
          </>
        )}
        {run.decision && (
          <Badge variant="outline">
            {DECISION_LABELS[run.decision]} by {run.decidedBy}
            {run.decidedAt ? `, ${formatInstant(run.decidedAt)}` : ''}
          </Badge>
        )}
      </div>
      {run.status === 'ROUTING_FAILED' && (
        <Alert variant="destructive">
          <AlertTitle>Routing failed: {run.failureCode}</AlertTitle>
          <AlertDescription>
            <p>{run.failureMessage}</p>
            <p>No proposal was produced. Correct or remove the stop and evaluate again.</p>
          </AlertDescription>
        </Alert>
      )}
      {run.status === 'COMPLETED' && infeasible.length > 0 && (
        <Alert>
          <AlertTitle>Infeasible methods</AlertTitle>
          <AlertDescription>
            <ul className="list-disc pl-4">
              {infeasible.map((r) => (
                <li key={r.method}>
                  {METHOD_LABELS[r.method]}: {r.infeasibilityReason}
                </li>
              ))}
            </ul>
          </AlertDescription>
        </Alert>
      )}
    </div>
  )
}
