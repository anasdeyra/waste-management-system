import { useQuery } from '@tanstack/react-query'

import { Badge } from '@/components/ui/badge'
import { ErrorBlock, LoadingBlock, PageHeader } from '@/components/layout/page-header'
import { DataTable } from '@/components/shared/data-table'
import { api } from '@/lib/api'
import { formatKg, humanize } from '@/lib/format'
import type { CollectionPoint, CollectionPointStatus } from '@/types/api'

const statusVariant: Record<CollectionPointStatus, 'default' | 'secondary' | 'outline' | 'destructive'> = {
  ACTIVE: 'default',
  FULL: 'secondary',
  INACTIVE: 'outline',
}

export function CollectionPointsPage() {
  const points = useQuery({
    queryKey: ['collection-points'],
    queryFn: api.collectionPoints.list,
  })

  return (
    <div>
      <PageHeader
        title="Collection points"
        description="Georeferenced waste collection points with their capacity and demand."
      />

      {points.isLoading ? <LoadingBlock /> : null}
      {points.isError ? <ErrorBlock error={points.error} /> : null}

      {points.data ? (
        <DataTable<CollectionPoint>
          rows={points.data}
          rowKey={(row) => row.id}
          emptyMessage="No collection points registered."
          columns={[
            { key: 'name', header: 'Name', cell: (row) => row.name },
            { key: 'address', header: 'Address', cell: (row) => row.address },
            {
              key: 'category',
              header: 'Waste category',
              cell: (row) => row.wasteCategoryName,
            },
            {
              key: 'load',
              header: 'Load',
              cell: (row) => `${formatKg(row.currentLoadKg)} / ${formatKg(row.capacityKg)}`,
            },
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
