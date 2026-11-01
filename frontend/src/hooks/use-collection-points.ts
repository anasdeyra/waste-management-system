import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api-client'
import type {
  CollectionPoint,
  CollectionPointStatus,
  CollectionPointStatusRequest,
  CreateCollectionPointRequest,
  DemandEntry,
  DemandEntryRequest,
  Depot,
  UpdateCollectionPointRequest,
  WasteCategory,
} from '@/types/api'

export interface PointFilters {
  status?: CollectionPointStatus
  categoryId?: string
  eligible?: boolean
}

const pointsKey = ['collection-points'] as const

export function useCollectionPoints(filters: PointFilters) {
  return useQuery({
    queryKey: [
      ...pointsKey,
      { status: filters.status, categoryId: filters.categoryId, eligible: filters.eligible },
    ],
    queryFn: () => {
      const params = new URLSearchParams()
      if (filters.status) params.set('status', filters.status)
      if (filters.categoryId) params.set('categoryId', filters.categoryId)
      if (filters.eligible !== undefined) params.set('eligible', String(filters.eligible))
      const qs = params.toString()
      return apiFetch<CollectionPoint[]>(`/api/collection-points${qs ? `?${qs}` : ''}`)
    },
  })
}

export function useDepots() {
  return useQuery({
    queryKey: ['depots'],
    queryFn: () => apiFetch<Depot[]>('/api/depots'),
  })
}

/** Every waste category, inactive ones included (they may still carry demand). */
export function useAllCategories() {
  return useQuery({
    queryKey: ['waste-categories', { includeInactive: true }],
    queryFn: () => apiFetch<WasteCategory[]>('/api/waste-categories'),
  })
}

function useInvalidatingMutation<TVars, TData>(fn: (v: TVars) => Promise<TData>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: pointsKey }),
  })
}

export function useCreatePoint() {
  return useInvalidatingMutation((body: CreateCollectionPointRequest) =>
    apiFetch<CollectionPoint>('/api/collection-points', { method: 'POST', body }),
  )
}

export function useUpdatePoint() {
  return useInvalidatingMutation((v: { id: string; body: UpdateCollectionPointRequest }) =>
    apiFetch<CollectionPoint>(`/api/collection-points/${v.id}`, { method: 'PUT', body: v.body }),
  )
}

export function useSetPointStatus() {
  return useInvalidatingMutation((v: { id: string; status: CollectionPointStatus }) =>
    apiFetch<CollectionPoint>(`/api/collection-points/${v.id}/status`, {
      method: 'PATCH',
      body: { status: v.status } satisfies CollectionPointStatusRequest,
    }),
  )
}

export function useAddDemand() {
  return useInvalidatingMutation((v: { pointId: string; body: DemandEntryRequest }) =>
    apiFetch<DemandEntry>(`/api/collection-points/${v.pointId}/demands`, {
      method: 'POST',
      body: v.body,
    }),
  )
}

export function useUpdateDemand() {
  return useInvalidatingMutation(
    (v: { pointId: string; demandId: string; body: DemandEntryRequest }) =>
      apiFetch<DemandEntry>(`/api/collection-points/${v.pointId}/demands/${v.demandId}`, {
        method: 'PUT',
        body: v.body,
      }),
  )
}

export function useRemoveDemand() {
  return useInvalidatingMutation((v: { pointId: string; demandId: string }) =>
    apiFetch<void>(`/api/collection-points/${v.pointId}/demands/${v.demandId}`, {
      method: 'DELETE',
    }),
  )
}
