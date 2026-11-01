import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api-client'
import type { StopOutcomeRequest, Tour, TourStatus, TourSummary } from '@/types/api'

export const myToursKey = ['my-tours'] as const
const myTourKey = (id: string) => ['my-tours', id] as const

/** The signed-in agent's assigned, non-draft tours (GET /api/tours/mine). */
export function useMyTours(status?: TourStatus) {
  return useQuery({
    queryKey: [...myToursKey, 'list', { status }],
    queryFn: () =>
      apiFetch<TourSummary[]>(`/api/tours/mine${status ? `?status=${status}` : ''}`),
  })
}

export function useMyTour(id: string | undefined) {
  return useQuery({
    queryKey: myTourKey(id ?? ''),
    queryFn: () => apiFetch<Tour>(`/api/tours/mine/${id}`),
    enabled: !!id,
  })
}

/** Every execution call returns the whole tour: write it into the cache. */
function useExecutionMutation<TVars>(fn: (v: TVars) => Promise<Tour>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: (tour) => {
      queryClient.setQueryData(myTourKey(tour.id), tour)
      return queryClient.invalidateQueries({ queryKey: [...myToursKey, 'list'] })
    },
  })
}

export function useStartTour() {
  return useExecutionMutation((v: { id: string }) =>
    apiFetch<Tour>(`/api/tours/${v.id}/start`, { method: 'POST' }),
  )
}

export function useRecordStopOutcome() {
  return useExecutionMutation((v: { id: string; stopId: string; body: StopOutcomeRequest }) =>
    apiFetch<Tour>(`/api/tours/${v.id}/stops/${v.stopId}/outcome`, {
      method: 'POST',
      body: v.body,
    }),
  )
}

export function useCompleteTour() {
  return useExecutionMutation((v: { id: string }) =>
    apiFetch<Tour>(`/api/tours/${v.id}/completion`, { method: 'POST' }),
  )
}
