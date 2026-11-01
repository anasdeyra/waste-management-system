import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api-client'
import type {
  ResourceStatus,
  ResourceStatusRequest,
  Vehicle,
  VehicleRequest,
} from '@/types/api'

const vehiclesKey = ['vehicles'] as const

export function useVehicles(status?: ResourceStatus) {
  return useQuery({
    queryKey: [...vehiclesKey, { status }],
    queryFn: () => apiFetch<Vehicle[]>(`/api/vehicles${status ? `?status=${status}` : ''}`),
  })
}

function useVehicleMutation<TVars>(fn: (v: TVars) => Promise<Vehicle>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: vehiclesKey }),
  })
}

export function useCreateVehicle() {
  return useVehicleMutation((body: VehicleRequest) =>
    apiFetch<Vehicle>('/api/vehicles', { method: 'POST', body }),
  )
}

export function useUpdateVehicle() {
  return useVehicleMutation((v: { id: string; body: VehicleRequest }) =>
    apiFetch<Vehicle>(`/api/vehicles/${v.id}`, { method: 'PUT', body: v.body }),
  )
}

export function useSetVehicleStatus() {
  return useVehicleMutation((v: { id: string; status: ResourceStatus }) =>
    apiFetch<Vehicle>(`/api/vehicles/${v.id}/status`, {
      method: 'PATCH',
      body: { status: v.status } satisfies ResourceStatusRequest,
    }),
  )
}
