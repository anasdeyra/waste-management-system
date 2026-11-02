import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'

import { BatchSummary } from '@/components/evaluations/batch-summary'
import { CsvImportCard } from '@/components/evaluations/csv-import-card'
import { ExperimentLauncher } from '@/components/evaluations/experiment-launcher'
import { RunFilterBar, type FilterValues } from '@/components/evaluations/run-filters'
import { RunTable } from '@/components/evaluations/run-table'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
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
import { useCurrentUser } from '@/hooks/use-auth'
import {
  useEvaluationBatch,
  useEvaluationBatches,
  useEvaluationRuns,
} from '@/hooks/use-evaluation-explorer'
import { apiDownload } from '@/lib/api-client'
import { hasRole } from '@/lib/auth'
import { formatInstant } from '@/lib/evaluation-format'
import { batchKindLabel, BATCH_STATUS_LABELS } from '@/lib/explorer-labels'

const PAGE_SIZE = 25

/** Evaluation explorer (US-36, US-37, US-38): batches, runs, CSV exchange and the experiment launcher. */
export default function EvaluationExplorerPage() {
  const { data: user } = useCurrentUser()
  const [params] = useSearchParams()
  const [selected, setSelected] = useState<string | undefined>(() => params.get('batch') ?? undefined)
  const [filters, setFilters] = useState<FilterValues>({})
  const [page, setPage] = useState(0)
  const batches = useEvaluationBatches()
  const detail = useEvaluationBatch(selected)
  const runs = useEvaluationRuns({ ...filters, page, size: PAGE_SIZE })

  const select = (id: string) => setSelected(id)
  const exportAll = () => {
    const q = new URLSearchParams()
    if (filters.kind) q.set('kind', filters.kind)
    if (filters.batchId) q.set('batchId', filters.batchId)
    const qs = q.toString()
    apiDownload(`/api/evaluations/export${qs ? `?${qs}` : ''}`, 'evaluation-results-all.csv').catch((e: Error) =>
      toast.error(e.message),
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-xl font-semibold">Evaluation explorer</h1>
        <p className="text-sm text-muted-foreground">
          Retained three-method comparisons. The hand-crafted demo tours are listed as tour evaluations and are never part of an experiment summary.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Evaluation batches</CardTitle>
        </CardHeader>
        <CardContent>
          {batches.isPending && <Skeleton className="h-16" />}
          {batches.error && (
            <Alert variant="destructive">
              <AlertTitle>Could not load the batches</AlertTitle>
              <AlertDescription>{batches.error.message}</AlertDescription>
            </Alert>
          )}
          {batches.data && batches.data.length === 0 && (
            <p className="text-sm text-muted-foreground">No evaluation batches yet.</p>
          )}
          {batches.data && batches.data.length > 0 && (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Created</TableHead>
                    <TableHead>Label</TableHead>
                    <TableHead>Kind</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Runs completed / routing failed</TableHead>
                    <TableHead>Created by</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {batches.data.map((b) => (
                    <TableRow
                      key={b.id}
                      className="cursor-pointer"
                      data-state={b.id === selected ? 'selected' : undefined}
                      onClick={() => select(b.id)}
                    >
                      <TableCell>{formatInstant(b.createdAt)}</TableCell>
                      <TableCell>{b.label}</TableCell>
                      <TableCell>{batchKindLabel(b)}</TableCell>
                      <TableCell>
                        <Badge variant={b.status === 'ABORTED' ? 'destructive' : 'secondary'}>
                          {BATCH_STATUS_LABELS[b.status]}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {b.completedRunCount} / {b.failedRunCount}
                      </TableCell>
                      <TableCell>{b.createdBy}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {selected && detail.isPending && <Skeleton className="h-40" />}
      {selected && detail.data && <BatchSummary detail={detail.data} />}

      <Card>
        <CardHeader>
          <CardTitle>Runs</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <RunFilterBar
            value={filters}
            batches={batches.data ?? []}
            onChange={(v) => {
              setFilters(v)
              setPage(0)
            }}
          />
          {runs.isPending && <Skeleton className="h-24" />}
          {runs.error && (
            <Alert variant="destructive">
              <AlertTitle>Could not load the runs</AlertTitle>
              <AlertDescription>{runs.error.message}</AlertDescription>
            </Alert>
          )}
          {runs.data && runs.data.items.length === 0 && (
            <p className="text-sm text-muted-foreground">No evaluation runs match these filters.</p>
          )}
          {runs.data && runs.data.items.length > 0 && <RunTable items={runs.data.items} />}
          {runs.data && (
            <div className="flex flex-wrap items-center gap-2">
              <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                Previous
              </Button>
              <span className="text-sm">
                Page {runs.data.page + 1} of {Math.max(runs.data.totalPages, 1)}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= runs.data.totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
              <Button variant="outline" size="sm" className="ml-auto" onClick={exportAll}>
                Export all results CSV
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <CsvImportCard onImported={select} />
      {hasRole(user, 'ADMIN') && <ExperimentLauncher onDone={select} />}
    </div>
  )
}
