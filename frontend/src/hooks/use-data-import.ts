import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch, ApiRequestError } from '@/lib/api-client'
import type { ImportReport } from '@/types/api'

const reportKey = ['admin', 'import-report'] as const

/** The last import report, or null when no import has run since the backend started (404). */
export function useImportReport() {
  return useQuery({
    queryKey: reportKey,
    retry: false,
    queryFn: async () => {
      try {
        return await apiFetch<ImportReport>('/api/admin/import/report')
      } catch (err) {
        if (err instanceof ApiRequestError && err.status === 404) return null
        throw err
      }
    },
  })
}

export function useRunImport() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiFetch<ImportReport>('/api/admin/import', { method: 'POST' }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: reportKey })
      await queryClient.invalidateQueries({ queryKey: ['collection-points'] })
    },
  })
}
