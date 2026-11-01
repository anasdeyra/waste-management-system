import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'

import { fetchCurrentUser, login, logout } from '@/lib/auth'

export const currentUserQueryKey = ['auth', 'me'] as const

/** The TanStack Query cache is the auth store: null means signed out. */
export function useCurrentUser() {
  return useQuery({
    queryKey: currentUserQueryKey,
    queryFn: fetchCurrentUser,
    staleTime: Infinity,
    retry: false,
  })
}

export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationKey: ['login'],
    mutationFn: login,
    onSuccess: (user) => queryClient.setQueryData(currentUserQueryKey, user),
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationKey: ['logout'],
    mutationFn: logout,
    onSettled: () => {
      queryClient.clear()
      queryClient.setQueryData(currentUserQueryKey, null)
      navigate('/login', { replace: true })
    },
  })
}
