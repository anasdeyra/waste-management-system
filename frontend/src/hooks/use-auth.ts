import { createContext, useContext } from 'react'

import type { LoginRequest, UserProfile } from '@/types/api'

/** Value exposed by the authentication context. */
export type AuthContextValue = {
  user: UserProfile | null
  isAuthenticated: boolean
  /** `true` while the initial session probe is running. */
  isLoading: boolean
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

/** Returns the authentication state of the current session. */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside an <AuthProvider>')
  }
  return context
}
