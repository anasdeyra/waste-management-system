import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { evaluationsKey } from '@/hooks/use-evaluations'
import { apiFetch } from '@/lib/api-client'
import type {
  AddTourStopRequest,
  CancelTourRequest,
  OutcomeCorrectionRequest,
  ProposalDecisionRequest,
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
function useTourMutation<TVars>(
  fn: (v: TVars) => Promise<Tour>,
  alsoInvalidate: readonly (readonly unknown[])[] = [],
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: (tour) => {
      queryClient.setQueryData(tourKey(tour.id), tour)
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: [...toursKey, 'list'] }),
        ...alsoInvalidate.map((queryKey) => queryClient.invalidateQueries({ queryKey })),
      ])
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

/** Applies the NN + 2-opt order and road route of a current run (POST .../proposal-acceptances). */
export function useAcceptProposal() {
  return useTourMutation(
    (v: { id: string; body: ProposalDecisionRequest }) =>
      apiFetch<Tour>(`/api/tours/${v.id}/proposal-acceptances`, { method: 'POST', body: v.body }),
    [evaluationsKey],
  )
}

/** Keeps the input order and attaches its road route (POST .../proposal-rejections). */
export function useRejectProposal() {
  return useTourMutation(
    (v: { id: string; body: ProposalDecisionRequest }) =>
      apiFetch<Tour>(`/api/tours/${v.id}/proposal-rejections`, { method: 'POST', body: v.body }),
    [evaluationsKey],
  )
}

/** DRAFT -> PLANNED after full revalidation (POST .../confirmation). */
export function useConfirmTour() {
  return useTourMutation((v: { id: string }) =>
    apiFetch<Tour>(`/api/tours/${v.id}/confirmation`, { method: 'POST' }),
  )
}

/** DRAFT, PLANNED or IN_PROGRESS -> CANCELLED with a reason (POST .../cancellation). */
export function useCancelTour() {
  return useTourMutation(
    (v: { id: string; body: CancelTourRequest }) =>
      apiFetch<Tour>(`/api/tours/${v.id}/cancellation`, { method: 'POST', body: v.body }),
    [historyKey],
  )
}

/** Planner correction of a recorded stop outcome (POST .../stops/{stopId}/corrections). */
export function useCorrectStopOutcome() {
  return useTourMutation(
    (v: { id: string; stopId: string; body: OutcomeCorrectionRequest }) =>
      apiFetch<Tour>(`/api/tours/${v.id}/stops/${v.stopId}/corrections`, {
        method: 'POST',
        body: v.body,
      }),
    [historyKey],
  )
}

export const historyKey = ['tours', 'history'] as const

export interface HistoryFilters {
  from?: string
  to?: string
  status?: 'COMPLETED' | 'CANCELLED'
  vehicleId?: string
  agentId?: string
  pointId?: string
}

/** Completed and cancelled tours (GET /api/tours/history). */
export function useTourHistory(filters: HistoryFilters) {
  return useQuery({
    queryKey: [...historyKey, filters],
    queryFn: () => {
      const params = new URLSearchParams()
      for (const [k, v] of Object.entries(filters)) {
        if (v) params.set(k, v)
      }
      const qs = params.toString()
      return apiFetch<TourSummary[]>(`/api/tours/history${qs ? `?${qs}` : ''}`)
    },
  })
}
