import { useQuery } from '@tanstack/react-query'

import { Badge } from '@/components/ui/badge'
import { Card, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { ErrorBlock, LoadingBlock, PageHeader } from '@/components/layout/page-header'
import { DataTable } from '@/components/shared/data-table'
import { api } from '@/lib/api'
import { formatDateTime, formatKm, formatMinutes, formatNumber, humanize } from '@/lib/format'
import type { OptimizationRun } from '@/types/api'

export function OptimizationPage() {
  const runs = useQuery({
    queryKey: ['optimization', 'runs'],
    queryFn: api.optimization.runs,
  })

  return (
    <div>
      <PageHeader
        title="Tour optimization"
        description="Evaluation history of the intelligent module against the documented baseline."
      />

      {runs.isLoading ? <LoadingBlock /> : null}
      {runs.isError ? <ErrorBlock error={runs.error} /> : null}

      {runs.data ? (
        <div className="flex flex-col gap-6">
          <Card size="sm">
            <CardHeader>
              <CardTitle>Evaluation protocol</CardTitle>
              <CardDescription>
                Three deterministic planning methods are compared on identical scenarios: baseline
                (input order), capacity-aware nearest neighbour, and nearest neighbour followed by
                2-opt improvement.
              </CardDescription>
            </CardHeader>
          </Card>

          <DataTable<OptimizationRun>
            rows={runs.data}
            rowKey={(row) => row.id}
            emptyMessage="No optimization run recorded yet."
            columns={[
              { key: 'createdAt', header: 'Executed', cell: (row) => formatDateTime(row.createdAt) },
              { key: 'scenario', header: 'Scenario', cell: (row) => row.scenarioName },
              { key: 'points', header: 'Points', cell: (row) => formatNumber(row.pointCount) },
              {
                key: 'method',
                header: 'Method',
                cell: (row) => <Badge variant="outline">{humanize(row.method)}</Badge>,
              },
              { key: 'distance', header: 'Distance', cell: (row) => formatKm(row.totalDistanceKm) },
              {
                key: 'duration',
                header: 'Duration',
                cell: (row) => formatMinutes(row.estimatedDurationMinutes),
              },
              {
                key: 'execution',
                header: 'Execution time',
                cell: (row) => `${formatNumber(row.executionTimeMillis)} ms`,
              },
              {
                key: 'violations',
                header: 'Violations',
                cell: (row) => formatNumber(row.constraintViolations),
              },
            ]}
          />
        </div>
      ) : null}
    </div>
  )
}
