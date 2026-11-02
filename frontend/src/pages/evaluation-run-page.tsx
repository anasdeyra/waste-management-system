import { toast } from 'sonner'
import { Link, useParams } from 'react-router-dom'

import { EvaluationComparison } from '@/components/tours/evaluation-comparison'
import { formatCoordinates } from '@/components/maps/geo'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useEvaluation } from '@/hooks/use-evaluations'
import { apiDownload } from '@/lib/api-client'
import { formatInstant } from '@/lib/evaluation-format'
import { METHOD_LABELS } from '@/lib/evaluation-labels'
import { SCENARIO_KIND_LABELS } from '@/lib/explorer-labels'

function Item({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="break-words text-sm">{children ?? '—'}</dd>
    </div>
  )
}

/** One retained evaluation run: provenance, comparison map and stops (US-36). */
export default function EvaluationRunPage() {
  const { runId } = useParams()
  const { data: run, error, isPending } = useEvaluation(runId)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">Evaluation run</h1>
          <Link to="/evaluations" className="text-sm underline underline-offset-2">
            Evaluation explorer
          </Link>
        </div>
        {run && (
          <Button
            variant="outline"
            onClick={() =>
              apiDownload(`/api/evaluations/export?runId=${run.id}`, `evaluation-results-run-${run.id}.csv`).catch(
                (e: Error) => toast.error(e.message),
              )
            }
          >
            Export this run (CSV)
          </Button>
        )}
      </div>
      {isPending && <Skeleton className="h-40" />}
      {error && (
        <Alert variant="destructive">
          <AlertTitle>Could not load the run</AlertTitle>
          <AlertDescription>{error.message}</AlertDescription>
        </Alert>
      )}
      {run && (
        <>
          <Card>
            <CardHeader>
              <CardTitle>Reproducibility</CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                <Item label="Kind">{SCENARIO_KIND_LABELS[run.scenarioKind]}</Item>
                <Item label="Scenario">
                  {run.scenarioKey ??
                    (run.tourId ? (
                      <Link to={`/tours/${run.tourId}`} className="underline underline-offset-2">
                        Open tour
                      </Link>
                    ) : undefined)}
                </Item>
                <Item label="Demand profile">{run.demandProfile}</Item>
                <Item label="Seed">{run.seed}</Item>
                <Item label="Repetition">{run.repetition}</Item>
                <Item label="Batch">
                  {run.batchId ? (
                    <Link to={`/evaluations?batch=${run.batchId}`} className="underline underline-offset-2">
                      Open batch
                    </Link>
                  ) : undefined}
                </Item>
                <Item label="Status">
                  {run.status === 'ROUTING_FAILED' ? `Routing failed (${run.failureCode})` : 'Completed'}
                </Item>
                <Item label="Created">
                  {run.createdBy}, {formatInstant(run.createdAt)}
                </Item>
                <Item label="Vehicle capacity">{run.vehicleCapacityKg} kg</Item>
                <Item label="Total demand">{run.totalDemandKg} kg</Item>
                <Item label="Service time">{run.serviceTimeSeconds} s per stop</Item>
                <Item label="Source snapshots">{run.sourceSnapshotIds.join('; ')}</Item>
                <Item label="Road graph version">{run.roadGraphVersion}</Item>
                <Item label="Routing engine">{run.routingEngine}</Item>
              </dl>
              {run.results.length > 0 && (
                <ul className="mt-3 flex flex-col gap-1 text-xs text-muted-foreground">
                  {run.results.map((r) => (
                    <li key={r.method}>
                      <span className="font-medium">{METHOD_LABELS[r.method]}:</span> {r.algorithmConfiguration}
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          {run.status === 'ROUTING_FAILED' ? (
            <Alert variant="destructive">
              <AlertTitle>Routing failed</AlertTitle>
              <AlertDescription>{run.failureMessage}</AlertDescription>
            </Alert>
          ) : (
            <EvaluationComparison run={run} />
          )}

          <Card>
            <CardHeader>
              <CardTitle>Stops</CardTitle>
            </CardHeader>
            <CardContent className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Input order</TableHead>
                    <TableHead>Source id</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Location</TableHead>
                    <TableHead>Demand</TableHead>
                    <TableHead>Demand breakdown</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {run.stops.map((s) => (
                    <TableRow key={s.inputIndex}>
                      <TableCell>{s.inputIndex}</TableCell>
                      <TableCell>{s.sourceIdentifier ?? '—'}</TableCell>
                      <TableCell>{s.name}</TableCell>
                      <TableCell>{formatCoordinates({ longitude: s.longitude, latitude: s.latitude })}</TableCell>
                      <TableCell>{s.demandKg} kg</TableCell>
                      <TableCell>{s.demandBreakdown ?? '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  )
}
