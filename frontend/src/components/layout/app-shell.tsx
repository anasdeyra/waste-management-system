import { NavLink, Outlet } from 'react-router-dom'
import {
  Boxes,
  Gauge,
  LayoutDashboard,
  LogOut,
  MapPin,
  Menu,
  Route,
  Trash2,
  Truck,
  Users,
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { Separator } from '@/components/ui/separator'
import { useAuth } from '@/hooks/use-auth'

type NavItem = {
  to: string
  label: string
  icon: typeof LayoutDashboard
  end?: boolean
}

const navSections: { title: string; items: NavItem[] }[] = [
  {
    title: 'Overview',
    items: [{ to: '/', label: 'Dashboard', icon: LayoutDashboard, end: true }],
  },
  {
    title: 'Operations',
    items: [
      { to: '/collection-points', label: 'Collection points', icon: MapPin },
      { to: '/vehicles', label: 'Vehicles', icon: Truck },
      { to: '/agents', label: 'Agents', icon: Users },
      { to: '/tours', label: 'Tours', icon: Route },
    ],
  },
  {
    title: 'Reference & intelligence',
    items: [
      { to: '/waste-categories', label: 'Waste categories', icon: Trash2 },
      { to: '/optimization', label: 'Tour optimization', icon: Gauge },
    ],
  },
]

function Brand() {
  return (
    <div className="flex items-center gap-2">
      <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
        <Boxes className="size-4" />
      </span>
      <div className="leading-tight">
        <p className="text-sm font-medium">WasteMS</p>
        <p className="text-xs text-muted-foreground">Intelligent waste management</p>
      </div>
    </div>
  )
}

function Navigation({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav className="flex flex-col gap-6 px-2">
      {navSections.map((section) => (
        <div key={section.title} className="flex flex-col gap-1">
          <p className="px-2 text-xs font-medium uppercase tracking-wide text-muted-foreground">
            {section.title}
          </p>
          {section.items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={onNavigate}
              className={({ isActive }) =>
                [
                  'flex items-center gap-2 rounded-lg px-2 py-1.5 text-sm transition-colors',
                  isActive
                    ? 'bg-primary/10 font-medium text-primary'
                    : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                ].join(' ')
              }
            >
              <item.icon className="size-4" />
              {item.label}
            </NavLink>
          ))}
        </div>
      ))}
    </nav>
  )
}

function UserPanel() {
  const { user, logout } = useAuth()

  return (
    <div className="flex flex-col gap-3 px-2">
      <Separator />
      <div className="flex items-center justify-between gap-2">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{user?.fullName ?? 'Signed in'}</p>
          <p className="truncate text-xs text-muted-foreground">{user?.username ?? '—'}</p>
        </div>
        <Button variant="ghost" size="icon" onClick={() => void logout()} title="Sign out">
          <LogOut className="size-4" />
        </Button>
      </div>
    </div>
  )
}

export function AppShell() {
  return (
    <div className="flex min-h-svh bg-background text-foreground">
      {/* Desktop sidebar */}
      <aside className="hidden w-64 shrink-0 flex-col justify-between border-r bg-card/40 p-4 lg:flex">
        <div className="flex flex-col gap-6">
          <Brand />
          <Navigation />
        </div>
        <UserPanel />
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        {/* Mobile header */}
        <header className="flex items-center gap-2 border-b px-4 py-3 lg:hidden">
          <Sheet>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon">
                <Menu className="size-4" />
              </Button>
            </SheetTrigger>
            <SheetContent side="left" className="w-72">
              <SheetHeader>
                <SheetTitle>
                  <Brand />
                </SheetTitle>
              </SheetHeader>
              <div className="flex flex-1 flex-col justify-between gap-6 overflow-y-auto py-4">
                <Navigation />
                <UserPanel />
              </div>
            </SheetContent>
          </Sheet>
          <Brand />
        </header>

        <main className="min-w-0 flex-1 p-4 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
