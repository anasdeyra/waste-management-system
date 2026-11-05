import { Boxes, LogOut } from 'lucide-react'
import { Link, Outlet } from 'react-router-dom'

import { MainNav } from '@/components/shared/main-nav'
import { RoleBadgeCard } from '@/components/shared/role-badge-card'
import { Button } from '@/components/ui/button'
import { useCurrentUser, useLogout } from '@/hooks/use-auth'

/** Layout of every signed-in page: header with role-aware navigation, then the page. */
export function AppShell() {
  const { data: user } = useCurrentUser()
  const logout = useLogout()

  return (
    <div className="min-h-svh bg-muted/30">
      <header className="border-b bg-background">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-4 gap-y-2 p-3 md:px-6">
          <Link to="/" className="flex items-center gap-2 font-semibold">
            <Boxes className="size-5" />
            WasteMS
          </Link>
          <MainNav />
          <div className="ml-auto flex items-center gap-2">
            <span className="text-sm">{user?.username}</span>
            {user && <RoleBadgeCard user={user} />}
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
