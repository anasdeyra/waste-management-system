import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api-client'
import type { AuditAction, AuditEntityType, AuditEventPage, AuditOutcome } from '@/types/api'

export interface AuditFilters {
  from?: string
  to?: string
  actor?: string
  action?: AuditAction
  entityType?: AuditEntityType
  entityId?: string
  outcome?: AuditOutcome
  page: number
  size: number
}

/** One page of the audit log, newest first (GET /api/admin/audit-events, ADMIN only). */
export function useAuditEvents(filters: AuditFilters) {
  return useQuery({
    queryKey: ['audit-events', filters],
    queryFn: () => {
      const params = new URLSearchParams()
      for (const [k, v] of Object.entries(filters)) {
        if (v !== undefined && v !== '') params.set(k, String(v))
      }
      return apiFetch<AuditEventPage>(`/api/admin/audit-events?${params.toString()}`)
    },
    placeholderData: keepPreviousData,
  })
}
