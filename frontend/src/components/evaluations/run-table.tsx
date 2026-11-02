import { useNavigate } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatDistanceKm, formatExecutionMs, formatInstant } from '@/lib/evaluation-format'
import { formatChangePct, SCENARIO_KIND_LABELS } from '@/lib/explorer-labels'
import type { EvaluationMethodBrief, EvaluationRunListItem } from '@/types/api'

function Change({ value }: { value?: number }) {
  return value === undefined ? (
    <span title="Not comparable (infeasible or failed)">—</span>
  ) : (
    <>{formatChangePct(value)}</>
  )
}

/** Runs of the explorer list; a row opens the run page. */
export function RunTable({ items }: { items: EvaluationRunListItem[] }) {
  const navigate = useNavigate()
  const method = (r: EvaluationRunListItem, m: EvaluationMethodBrief['method']) =>
    r.results.find((x) => x.method === m)
  return (
    <div className="overflow-x-auto">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Created</TableHead>
            <TableHead>Kind</TableHead>
            <TableHead>Scenario</TableHead>
            <TableHead>Points</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Input-order distance</TableHead>
            <TableHead>Sweep change</TableHead>
            <TableHead>NN change</TableHead>
            <TableHead>NN + 2-opt change</TableHead>
            <TableHead>NN + 2-opt change vs sweep</TableHead>
            <TableHead>NN + 2-opt duration change</TableHead>
            <TableHead>NN + 2-opt execution time</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((r) => {
            const io = method(r, 'INPUT_ORDER')
            const sweep = method(r, 'SWEEP')
            const nn = method(r, 'NEAREST_NEIGHBOR')
            const opt = method(r, 'NN_PLUS_2_OPT')
            return (
              <TableRow key={r.id} className="cursor-pointer" onClick={() => navigate(`/evaluations/${r.id}`)}>
                <TableCell>{formatInstant(r.createdAt)}</TableCell>
                <TableCell>{SCENARIO_KIND_LABELS[r.scenarioKind]}</TableCell>
                <TableCell>{r.scenarioKey ?? `Tour ${(r.tourId ?? '').slice(0, 8)}`}</TableCell>
                <TableCell>{r.pointCount}</TableCell>
                <TableCell>
                  <Badge variant={r.status === 'ROUTING_FAILED' ? 'destructive' : 'secondary'}>
                    {r.status === 'ROUTING_FAILED' ? 'Routing failed' : 'Completed'}
                  </Badge>
                </TableCell>
                <TableCell>{io?.feasible ? formatDistanceKm(io.distanceM) : '—'}</TableCell>
                <TableCell><Change value={sweep?.distanceChangePct} /></TableCell>
                <TableCell><Change value={nn?.distanceChangePct} /></TableCell>
                <TableCell><Change value={opt?.distanceChangePct} /></TableCell>
                <TableCell><Change value={opt?.distanceChangeVsSweepPct} /></TableCell>
                <TableCell><Change value={opt?.durationChangePct} /></TableCell>
                <TableCell>{opt ? formatExecutionMs(opt.executionTimeMs) : '—'}</TableCell>
              </TableRow>
            )
          })}
        </TableBody>
      </Table>
    </div>
  )
}
