import { apiFetch, ApiRequestError } from '@/lib/api-client'
import type { AppRole, CurrentUser, LoginRequest } from '@/types/api'

/** Returns the signed-in user, or null when there is no valid session. */
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  try {
    return await apiFetch<CurrentUser>('/api/auth/me')
  } catch (err) {
    if (err instanceof ApiRequestError && err.status === 401) return null
    throw err
  }
}

export function login(req: LoginRequest): Promise<CurrentUser> {
  return apiFetch<CurrentUser>('/api/auth/login', { method: 'POST', body: req })
}

export function logout(): Promise<void> {
  return apiFetch<void>('/api/auth/logout', { method: 'POST' })
}

/** Role check with the hierarchy ADMIN > PLANNER. UX only; the backend enforces access. */
export function hasRole(user: CurrentUser | null | undefined, required: AppRole): boolean {
  if (!user) return false
  return required === user.role || (required === 'PLANNER' && user.role === 'ADMIN')
}
