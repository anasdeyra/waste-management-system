import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { useCurrentUser } from '@/hooks/use-auth'
import { hasRole } from '@/lib/auth'
import type { AppRole } from '@/types/api'

/** Redirects to the login page when nobody is signed in. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { data: user, isPending } = useCurrentUser()
  const location = useLocation()

  if (isPending) {
    return (
      <div className="mx-auto flex max-w-md flex-col gap-3 p-6">
        <Skeleton className="h-8 w-full" />
        <Skeleton className="h-24 w-full" />
      </div>
    )
  }
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />
  return <>{children}</>
}

/** UX-only role gate; the backend enforces access (US-03). */
export function RequireRole({ role, children }: { role: AppRole; children: ReactNode }) {
  const { data: user } = useCurrentUser()
  if (!hasRole(user, role)) {
    return (
      <Alert variant="destructive">
        <AlertTitle>Access denied</AlertTitle>
        <AlertDescription>Your role does not allow this page.</AlertDescription>
      </Alert>
    )
  }
  return <>{children}</>
}
