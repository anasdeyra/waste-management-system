import { useQuery } from '@tanstack/react-query'

import { Badge } from '@/components/ui/badge'
import { ErrorBlock, LoadingBlock, PageHeader } from '@/components/layout/page-header'
import { DataTable } from '@/components/shared/data-table'
import { api } from '@/lib/api'
import type { WasteCategory } from '@/types/api'

export function WasteCategoriesPage() {
  const categories = useQuery({
    queryKey: ['waste-categories'],
    queryFn: api.wasteCategories.list,
  })

  return (
    <div>
      <PageHeader
        title="Waste categories"
        description="Reference data used to classify collection demand and collected waste."
      />

      {categories.isLoading ? <LoadingBlock /> : null}
      {categories.isError ? <ErrorBlock error={categories.error} /> : null}

      {categories.data ? (
        <DataTable<WasteCategory>
          rows={categories.data}
          rowKey={(row) => row.id}
          emptyMessage="No waste categories defined."
          columns={[
            { key: 'name', header: 'Name', cell: (row) => row.name },
            { key: 'description', header: 'Description', cell: (row) => row.description },
            {
              key: 'recyclable',
              header: 'Recyclable',
              cell: (row) => (
                <Badge variant={row.recyclable ? 'default' : 'outline'}>
                  {row.recyclable ? 'Yes' : 'No'}
                </Badge>
              ),
            },
          ]}
        />
      ) : null}
    </div>
  )
}
