import { useQuery } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api-client'
import type { OperationsDashboard } from '@/types/api'

export function useOperationsDashboard(period: { from?: string; to?: string }) {
  const q = new URLSearchParams()
  if (period.from) q.set('from', period.from)
  if (period.to) q.set('to', period.to)
  const qs = q.toString()
  return useQuery({
    queryKey: ['dashboard', 'operations', period.from ?? '', period.to ?? ''],
    queryFn: () => apiFetch<OperationsDashboard>(`/api/dashboard/operations${qs ? `?${qs}` : ''}`),
  })
}
