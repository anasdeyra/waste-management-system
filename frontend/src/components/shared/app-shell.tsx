import { Boxes, LogOut } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { useCurrentUser, useLogout } from '@/hooks/use-auth'
import { hasRole } from '@/lib/auth'
import { cn } from '@/lib/utils'

function navClass({ isActive }: { isActive: boolean }) {
  return cn(
    'rounded-md px-3 py-1.5 text-sm font-medium transition-colors',
    isActive ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-muted',
  )
}

/** Layout of every signed-in page: header with role-aware navigation, then the page. */
export function AppShell() {
  const { data: user } = useCurrentUser()
  const logout = useLogout()

  return (
    <div className="min-h-svh bg-muted/30">
      <header className="border-b bg-background">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-4 gap-y-2 p-3 md:px-6">
          <div className="flex items-center gap-2 font-semibold">
            <Boxes className="size-5" />
            WasteMS
          </div>
          <nav className="flex flex-wrap gap-1">
            <NavLink to="/" end className={navClass}>
              Home
            </NavLink>
            {hasRole(user, 'PLANNER') && (
              <NavLink to="/waste-categories" className={navClass}>
                Waste categories
              </NavLink>
            )}
            {hasRole(user, 'ADMIN') && (
              <NavLink to="/admin/users" className={navClass}>
                Users
              </NavLink>
            )}
          </nav>
          <div className="ml-auto flex items-center gap-2">
            <span className="text-sm">{user?.username}</span>
            {user && <Badge variant="secondary">{user.role}</Badge>}
            <Button
              variant="outline"
              size="sm"
              onClick={() => logout.mutate()}
              disabled={logout.isPending}
            >
              <LogOut />
              Sign out
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl p-4 md:p-6">
        <Outlet />
      </main>
    </div>
  )
}
