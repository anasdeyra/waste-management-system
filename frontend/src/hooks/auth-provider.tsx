import { useCallback, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

import { api, getToken, setToken } from '@/lib/api'
import type { LoginRequest, UserProfile } from '@/types/api'
import { AuthContext } from '@/hooks/use-auth'
import type { AuthContextValue } from '@/hooks/use-auth'

/**
 * Provides the authentication state to the application.
 *
 * <p>The token is kept in `localStorage`; on start-up it is probed against `/api/auth/me`
 * so that a page reload keeps the session.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(() => getToken() !== null)

  useEffect(() => {
    if (!getToken()) {
      return
    }

    let cancelled = false
    api.auth
      .me()
      .then((profile) => {
        if (!cancelled) {
          setUser(profile)
        }
      })
      .catch(() => {
        setToken(null)
        if (!cancelled) {
          setUser(null)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await api.auth.login(credentials)
    setToken(response.token)
    setUser(response.user)
  }, [])

  const logout = useCallback(async () => {
    try {
      await api.auth.logout()
    } catch {
      // A failed logout must never trap the user in the application.
    }
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isLoading,
      login,
      logout,
    }),
    [user, isLoading, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
