import { useQuery } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api-client'
import type { TrafficOverlay } from '@/types/api'

/** Stored NYC DOT snapshot (display only). Never touches the evaluation queries. */
export function useTrafficOverlay(enabled: boolean) {
  return useQuery({
    queryKey: ['traffic', 'overlay'],
    queryFn: () => apiFetch<TrafficOverlay>('/api/traffic/overlay'),
    enabled,
    staleTime: Infinity,
  })
}
