import { useQuery } from '@tanstack/react-query'

import { Badge } from '@/components/ui/badge'
import { ErrorBlock, LoadingBlock, PageHeader } from '@/components/layout/page-header'
import { DataTable } from '@/components/shared/data-table'
import { api } from '@/lib/api'
import { formatDate, formatKg, formatKm, formatMinutes, humanize } from '@/lib/format'
import type { Tour, TourStatus } from '@/types/api'

const statusVariant: Record<TourStatus, 'default' | 'secondary' | 'outline' | 'destructive'> = {
  PLANNED: 'secondary',
  IN_PROGRESS: 'default',
  COMPLETED: 'outline',
  CANCELLED: 'destructive',
}

export function ToursPage() {
  const tours = useQuery({
    queryKey: ['tours'],
    queryFn: api.tours.list,
  })

  return (
    <div>
      <PageHeader
        title="Tours"
        description="Planned collection tours, their crew and their measured performance."
      />

      {tours.isLoading ? <LoadingBlock /> : null}
      {tours.isError ? <ErrorBlock error={tours.error} /> : null}

      {tours.data ? (
        <DataTable<Tour>
          rows={tours.data}
          rowKey={(row) => row.id}
          emptyMessage="No tours planned yet."
          columns={[
            { key: 'date', header: 'Date', cell: (row) => formatDate(row.plannedDate) },
            { key: 'vehicle', header: 'Vehicle', cell: (row) => row.vehiclePlate },
            {
              key: 'crew',
              header: 'Crew',
              cell: (row) =>
                [row.driverName, ...row.collectorNames].filter(Boolean).join(', ') || '—',
            },
            { key: 'stops', header: 'Stops', cell: (row) => row.stops.length },
            { key: 'distance', header: 'Distance', cell: (row) => formatKm(row.totalDistanceKm) },
            {
              key: 'duration',
              header: 'Duration',
              cell: (row) => formatMinutes(row.estimatedDurationMinutes),
            },
            { key: 'load', header: 'Load', cell: (row) => formatKg(row.totalLoadKg) },
            {
              key: 'status',
              header: 'Status',
              cell: (row) => <Badge variant={statusVariant[row.status]}>{humanize(row.status)}</Badge>,
            },
          ]}
        />
      ) : null}
    </div>
  )
}
