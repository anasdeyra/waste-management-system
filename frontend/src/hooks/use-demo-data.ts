import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch, ApiRequestError } from '@/lib/api-client'
import type { DemoLoadReport } from '@/types/api'

const demoReportKey = ['admin', 'demo-report'] as const

/** The last demonstration report, or null when no load has run since the backend started (404). */
export function useDemoReport() {
  return useQuery({
    queryKey: demoReportKey,
    retry: false,
    queryFn: async () => {
      try {
        return await apiFetch<DemoLoadReport>('/api/admin/demo/report')
      } catch (err) {
        if (err instanceof ApiRequestError && err.status === 404) return null
        throw err
      }
    },
  })
}

export function useLoadDemo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiFetch<DemoLoadReport>('/api/admin/demo', { method: 'POST' }),
    onSuccess: async () => {
      await invalidateDemoData(queryClient)
    },
  })
}

/** Deletes the demonstration data and reloads it (user accounts and MN01 data are kept). */
export function useResetDemo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => apiFetch<DemoLoadReport>('/api/admin/demo/reset', { method: 'POST' }),
    onSuccess: async () => {
      await invalidateDemoData(queryClient)
    },
  })
}

async function invalidateDemoData(queryClient: ReturnType<typeof useQueryClient>) {
  await Promise.all(
    [
      demoReportKey,
      ['tours'],
      ['vehicles'],
      ['agents'],
      ['users'],
      ['collection-points'],
      ['waste-categories'],
      ['my-tours'],
    ].map((queryKey) => queryClient.invalidateQueries({ queryKey })),
  )
}
