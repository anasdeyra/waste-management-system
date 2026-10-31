import { useQuery } from '@tanstack/react-query'

import { Badge } from '@/components/ui/badge'
import { ErrorBlock, LoadingBlock, PageHeader } from '@/components/layout/page-header'
import { DataTable } from '@/components/shared/data-table'
import { api } from '@/lib/api'
import { formatKg, humanize } from '@/lib/format'
import type { Vehicle, VehicleStatus } from '@/types/api'

const statusVariant: Record<VehicleStatus, 'default' | 'secondary' | 'outline' | 'destructive'> = {
  AVAILABLE: 'default',
  IN_SERVICE: 'secondary',
  MAINTENANCE: 'destructive',
}

export function VehiclesPage() {
  const vehicles = useQuery({
    queryKey: ['vehicles'],
    queryFn: api.vehicles.list,
  })

  return (
    <div>
      <PageHeader title="Vehicles" description="Fleet of collection vehicles and their capacities." />

      {vehicles.isLoading ? <LoadingBlock /> : null}
      {vehicles.isError ? <ErrorBlock error={vehicles.error} /> : null}

      {vehicles.data ? (
        <DataTable<Vehicle>
          rows={vehicles.data}
          rowKey={(row) => row.id}
          emptyMessage="No vehicles registered."
          columns={[
            { key: 'plate', header: 'Plate', cell: (row) => row.plate },
            { key: 'description', header: 'Description', cell: (row) => row.description },
            { key: 'capacity', header: 'Capacity', cell: (row) => formatKg(row.capacityKg) },
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
