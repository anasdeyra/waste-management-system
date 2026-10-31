import { useQuery } from '@tanstack/react-query'

import { Badge } from '@/components/ui/badge'
import { ErrorBlock, LoadingBlock, PageHeader } from '@/components/layout/page-header'
import { DataTable } from '@/components/shared/data-table'
import { api } from '@/lib/api'
import { humanize } from '@/lib/format'
import type { Agent, AgentStatus } from '@/types/api'

const statusVariant: Record<AgentStatus, 'default' | 'secondary' | 'outline'> = {
  AVAILABLE: 'default',
  ASSIGNED: 'secondary',
  UNAVAILABLE: 'outline',
}

export function AgentsPage() {
  const agents = useQuery({
    queryKey: ['agents'],
    queryFn: api.agents.list,
  })

  return (
    <div>
      <PageHeader title="Agents" description="Drivers and collectors available for tour assignment." />

      {agents.isLoading ? <LoadingBlock /> : null}
      {agents.isError ? <ErrorBlock error={agents.error} /> : null}

      {agents.data ? (
        <DataTable<Agent>
          rows={agents.data}
          rowKey={(row) => row.id}
          emptyMessage="No agents registered."
          columns={[
            { key: 'name', header: 'Name', cell: (row) => row.fullName },
            { key: 'phone', header: 'Phone', cell: (row) => row.phone },
            { key: 'role', header: 'Role', cell: (row) => humanize(row.role) },
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
