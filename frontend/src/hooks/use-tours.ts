import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api-client'
import type {
  AddTourStopRequest,
  ReorderTourStopsRequest,
  Tour,
  TourAssignmentRequest,
  TourRequest,
  TourStatus,
  TourSummary,
} from '@/types/api'

export const toursKey = ['tours'] as const
export const tourKey = (id: string) => ['tours', id] as const

export interface TourFilters {
  status?: TourStatus
  planningDate?: string
}

export function useTours(filters: TourFilters) {
  return useQuery({
    queryKey: [...toursKey, 'list', { status: filters.status, planningDate: filters.planningDate }],
    queryFn: () => {
      const params = new URLSearchParams()
      if (filters.status) params.set('status', filters.status)
      if (filters.planningDate) params.set('planningDate', filters.planningDate)
      const qs = params.toString()
      return apiFetch<TourSummary[]>(`/api/tours${qs ? `?${qs}` : ''}`)
    },
  })
}

export function useTour(id: string | undefined) {
  return useQuery({
    queryKey: tourKey(id ?? ''),
    queryFn: () => apiFetch<Tour>(`/api/tours/${id}`),
    enabled: !!id,
  })
}

export function useCreateTour() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: TourRequest) => apiFetch<Tour>('/api/tours', { method: 'POST', body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: toursKey }),
  })
}

/** Every tour mutation returns the whole tour: write it into the cache (single source of truth). */
function useTourMutation<TVars>(fn: (v: TVars) => Promise<Tour>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: (tour) => {
      queryClient.setQueryData(tourKey(tour.id), tour)
      return queryClient.invalidateQueries({ queryKey: [...toursKey, 'list'] })
    },
  })
}

export function useUpdateTour() {
  return useTourMutation((v: { id: string; body: TourRequest }) =>
    apiFetch<Tour>(`/api/tours/${v.id}`, { method: 'PUT', body: v.body }),
  )
}

export function useAddTourStop() {
  return useTourMutation((v: { id: string; body: AddTourStopRequest }) =>
    apiFetch<Tour>(`/api/tours/${v.id}/stops`, { method: 'POST', body: v.body }),
  )
}

export function useRemoveTourStop() {
  return useTourMutation((v: { id: string; stopId: string }) =>
    apiFetch<Tour>(`/api/tours/${v.id}/stops/${v.stopId}`, { method: 'DELETE' }),
  )
}

export function useReorderTourStops() {
  return useTourMutation((v: { id: string; body: ReorderTourStopsRequest }) =>
    apiFetch<Tour>(`/api/tours/${v.id}/stops/order`, { method: 'PUT', body: v.body }),
  )
}

export function useAssignTour() {
  return useTourMutation((v: { id: string; body: TourAssignmentRequest }) =>
    apiFetch<Tour>(`/api/tours/${v.id}/assignment`, { method: 'PUT', body: v.body }),
  )
}
