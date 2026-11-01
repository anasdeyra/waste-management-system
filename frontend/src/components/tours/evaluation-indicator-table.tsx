import { Checkbox } from '@/components/ui/checkbox'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import {
  formatDistanceKm,
  formatDuration,
  formatExecutionMs,
  formatPercent,
  percentChange,
} from '@/lib/evaluation-format'
import { METHOD_LABELS, METHOD_ORDER, METHOD_STYLES } from '@/lib/evaluation-labels'
import type { EvaluationRun, MethodResult, PlanningMethod } from '@/types/api'

function checks(r: MethodResult) {
  if (!r.feasible) {
    return (
      <span className="text-destructive">Infeasible: {r.infeasibilityReason ?? 'no reason given'}</span>
    )
  }
  return (
    <span>
      {r.coverageOk ? 'Coverage ok' : 'Coverage failed'} /{' '}
      {r.capacityOk ? 'Capacity ok' : 'Capacity exceeded'}
    </span>
  )
}

/** Indicators of the three methods of one run, with changes relative to the input order. */
export function EvaluationIndicatorTable({
  run,
  visible,
  onToggle,
  numbersFor,
  onNumbersFor,
}: {
  run: EvaluationRun
  visible: Record<PlanningMethod, boolean>
  onToggle: (method: PlanningMethod, checked: boolean) => void
  numbersFor: PlanningMethod
  onNumbersFor: (method: PlanningMethod) => void
}) {
  const inputOrder = run.results.find((r) => r.method === 'INPUT_ORDER')
  const rows = METHOD_ORDER.map((m) => run.results.find((r) => r.method === m)).filter(
    (r): r is MethodResult => r !== undefined,
  )
  return (
    <div className="flex flex-col gap-3">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Show on map</TableHead>
            <TableHead>Numbers</TableHead>
            <TableHead>Method</TableHead>
            <TableHead>Distance</TableHead>
            <TableHead>Distance vs input order</TableHead>
            <TableHead>Duration</TableHead>
            <TableHead>Duration vs input order</TableHead>
            <TableHead>Execution time</TableHead>
            <TableHead>Checks</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.map((r) => {
            const m = r.method
            const baseline = m === 'INPUT_ORDER'
            return (
              <TableRow key={m}>
                <TableCell>
                  <Checkbox
                    checked={visible[m]}
                    disabled={!r.feasible}
                    aria-label={`Show ${METHOD_LABELS[m]} on map`}
                    onCheckedChange={(c) => onToggle(m, c === true)}
                  />
                </TableCell>
                <TableCell>
                  <input
                    type="radio"
                    name="numbers-for"
                    checked={numbersFor === m}
                    disabled={!r.feasible}
                    aria-label={`Number the stops in ${METHOD_LABELS[m]} order`}
                    onChange={() => onNumbersFor(m)}
                  />
                </TableCell>
                <TableCell>
                  <span
                    className="mr-2 inline-block size-3 rounded-sm align-middle"
                    style={{ backgroundColor: METHOD_STYLES[m].color }}
                  />
                  {METHOD_LABELS[m]}
                </TableCell>
                <TableCell>{formatDistanceKm(r.distanceM)}</TableCell>
                <TableCell>
                  {baseline ? 'baseline' : formatPercent(percentChange(r.distanceM, inputOrder?.distanceM))}
                </TableCell>
                <TableCell>{formatDuration(r.durationS)}</TableCell>
                <TableCell>
                  {baseline ? 'baseline' : formatPercent(percentChange(r.durationS, inputOrder?.durationS))}
                </TableCell>
                <TableCell>{formatExecutionMs(r.executionTimeMs)}</TableCell>
                <TableCell>{checks(r)}</TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
      <p className="text-xs text-muted-foreground">
        Distance is the directed road distance on road graph {run.roadGraphVersion}; duration = road
        travel time + {run.serviceTimeSeconds} s service per stop. Changes are relative to the input
        order of the same run (same scenario and formulas). Traffic is not an optimizer input (D-021).
      </p>
      <div className="flex flex-col gap-1 text-sm">
        {rows.map((r) => (
          <details key={r.method}>
            <summary className="cursor-pointer">Visit order — {METHOD_LABELS[r.method]}</summary>
            {r.feasible ? (
              <ol className="list-decimal pl-6">
                {r.visitOrder.map((i) => (
                  <li key={i}>{run.stops[i - 1].name}</li>
                ))}
              </ol>
            ) : (
              <p className="pl-6 text-muted-foreground">No order (infeasible)</p>
            )}
          </details>
        ))}
      </div>
    </div>
  )
}
