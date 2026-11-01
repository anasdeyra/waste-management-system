import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api-client'
import type {
  Agent,
  AgentRequest,
  LinkUserAccountRequest,
  ResourceStatus,
  ResourceStatusRequest,
} from '@/types/api'

const agentsKey = ['agents'] as const
const usersKey = ['users'] as const

export function useAgents(status?: ResourceStatus) {
  return useQuery({
    queryKey: [...agentsKey, { status }],
    queryFn: () => apiFetch<Agent[]>(`/api/agents${status ? `?status=${status}` : ''}`),
  })
}

function useAgentMutation<TVars, TData>(
  fn: (v: TVars) => Promise<TData>,
  alsoInvalidateUsers = false,
) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: agentsKey })
      if (alsoInvalidateUsers) await queryClient.invalidateQueries({ queryKey: usersKey })
    },
  })
}

export function useCreateAgent() {
  return useAgentMutation((body: AgentRequest) =>
    apiFetch<Agent>('/api/agents', { method: 'POST', body }),
  )
}

export function useUpdateAgent() {
  return useAgentMutation((v: { id: string; body: AgentRequest }) =>
    apiFetch<Agent>(`/api/agents/${v.id}`, { method: 'PUT', body: v.body }),
  )
}

export function useSetAgentStatus() {
  return useAgentMutation((v: { id: string; status: ResourceStatus }) =>
    apiFetch<Agent>(`/api/agents/${v.id}/status`, {
      method: 'PATCH',
      body: { status: v.status } satisfies ResourceStatusRequest,
    }),
  )
}

export function useLinkAgentAccount() {
  return useAgentMutation(
    (v: { agentId: string; userId: string }) =>
      apiFetch<Agent>(`/api/agents/${v.agentId}/user-account`, {
        method: 'PUT',
        body: { userId: v.userId } satisfies LinkUserAccountRequest,
      }),
    true,
  )
}

export function useUnlinkAgentAccount() {
  return useAgentMutation(
    (agentId: string) =>
      apiFetch<void>(`/api/agents/${agentId}/user-account`, { method: 'DELETE' }),
    true,
  )
}
