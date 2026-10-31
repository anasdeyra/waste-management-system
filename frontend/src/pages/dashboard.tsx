import { useQuery } from '@tanstack/react-query'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { PageHeader, ErrorBlock, LoadingBlock, StatCard } from '@/components/layout/page-header'
import { api } from '@/lib/api'
import { formatKg, formatNumber } from '@/lib/format'

export function DashboardPage() {
  const summary = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: api.dashboard.summary,
  })

  return (
    <div>
      <PageHeader
        title="Dashboard"
        description="Operational overview of collection points, resources and tours."
      />

      {summary.isLoading ? <LoadingBlock rows={2} /> : null}
      {summary.isError ? <ErrorBlock error={summary.error} title="Could not load the dashboard" /> : null}

      {summary.data ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              title="Collection points"
              value={formatNumber(summary.data.collectionPoints)}
              description={`${formatNumber(summary.data.activeCollectionPoints)} active`}
            />
            <StatCard
              title="Vehicles"
              value={formatNumber(summary.data.vehicles)}
              description={`${formatNumber(summary.data.availableVehicles)} available`}
            />
            <StatCard
              title="Agents"
              value={formatNumber(summary.data.agents)}
              description={`${formatNumber(summary.data.availableAgents)} available`}
            />
            <StatCard
              title="Tours today"
              value={formatNumber(summary.data.toursPlannedToday)}
              description={`${formatNumber(summary.data.toursInProgress)} in progress`}
            />
          </div>

          <Card className="mt-6">
            <CardHeader>
              <CardTitle>Collected waste</CardTitle>
              <CardDescription>Total mass recorded across completed tours.</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-semibold tracking-tight">
                {formatKg(summary.data.collectedKg)}
              </p>
            </CardContent>
          </Card>
        </>
      ) : null}
    </div>
  )
}
