import {
  ChevronDown,
  Database,
  FlaskConical,
  History,
  LayoutDashboard,
  MapPin,
  Recycle,
  RouteIcon,
  ScrollText,
  ShieldCheck,
  Truck,
  Upload,
  UserRound,
  Users,
  type LucideIcon,
} from 'lucide-react'
import { NavLink, useLocation } from 'react-router-dom'

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { useCurrentUser } from '@/hooks/use-auth'
import { hasRole } from '@/lib/auth'
import { cn } from '@/lib/utils'
import type { AppRole } from '@/types/api'

interface NavLeaf {
  label: string
  to: string
  icon: LucideIcon
}

interface NavGroup {
  label: string
  icon: LucideIcon
  items: NavLeaf[]
}

type NavEntry = (NavLeaf | NavGroup) & { role: AppRole }

/**
 * Role-aware navigation. Pages used every day are top-level links; the rest are grouped into
 * menus so that no role sees more than five entries. Role gates are UX only (the backend
 * enforces access), so `hasRole` keeps the ADMIN > PLANNER hierarchy.
 */
const navEntries: NavEntry[] = [
  { role: 'AGENT', label: 'My tours', to: '/my-tours', icon: RouteIcon },
  { role: 'PLANNER', label: 'Dashboard', to: '/dashboard', icon: LayoutDashboard },
  { role: 'PLANNER', label: 'Tours', to: '/tours', icon: RouteIcon },
  {
    role: 'PLANNER',
    label: 'Analysis',
    icon: FlaskConical,
    items: [
      { label: 'History', to: '/history', icon: History },
      { label: 'Evaluations', to: '/evaluations', icon: FlaskConical },
    ],
  },
  {
    role: 'PLANNER',
    label: 'Resources',
    icon: Database,
    items: [
      { label: 'Collection points', to: '/collection-points', icon: MapPin },
      { label: 'Vehicles', to: '/vehicles', icon: Truck },
      { label: 'Agents', to: '/agents', icon: UserRound },
      { label: 'Waste categories', to: '/waste-categories', icon: Recycle },
    ],
  },
  {
    role: 'ADMIN',
    label: 'Administration',
    icon: ShieldCheck,
    items: [
      { label: 'Users', to: '/admin/users', icon: Users },
      { label: 'Data import', to: '/admin/data-import', icon: Upload },
      { label: 'Audit log', to: '/admin/audit', icon: ScrollText },
    ],
  },
]

const itemClass =
  'inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors'
const activeClass = 'bg-primary text-primary-foreground'
const idleClass = 'text-muted-foreground hover:bg-muted'

function isGroup(entry: NavEntry): entry is NavGroup & { role: AppRole } {
  return 'items' in entry
}

/** True for the leaf's own path and any nested route such as /tours/:id. */
function isActivePath(pathname: string, to: string) {
  return pathname === to || pathname.startsWith(`${to}/`)
}

function NavGroupMenu({ group }: { group: NavGroup }) {
  const { pathname } = useLocation()
  const active = group.items.some((item) => isActivePath(pathname, item.to))
  const Icon = group.icon

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        className={cn(
          itemClass,
          'outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 data-[state=open]:bg-muted data-[state=open]:text-foreground',
          active ? activeClass : idleClass,
        )}
      >
        <Icon className="size-4" />
        {group.label}
        <ChevronDown className="size-3.5 opacity-70" />
      </DropdownMenuTrigger>
      <DropdownMenuContent className="w-auto min-w-48">
        {group.items.map((item) => {
          const ItemIcon = item.icon
          const itemActive = isActivePath(pathname, item.to)
          return (
            <DropdownMenuItem key={item.to} asChild>
              <NavLink
                to={item.to}
                className={cn('gap-2 px-2 py-1.5', itemActive && 'bg-muted font-medium')}
              >
                <ItemIcon className="text-muted-foreground" />
                {item.label}
              </NavLink>
            </DropdownMenuItem>
          )
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

/** Header navigation for the signed-in user's role. */
export function MainNav() {
  const { data: user } = useCurrentUser()
  const entries = navEntries.filter((entry) => hasRole(user, entry.role))

  return (
    <nav aria-label="Main" className="flex flex-wrap gap-1">
      {entries.map((entry) => {
        if (isGroup(entry)) return <NavGroupMenu key={entry.label} group={entry} />
        const Icon = entry.icon
        return (
          <NavLink
            key={entry.to}
            to={entry.to}
            className={({ isActive }) => cn(itemClass, isActive ? activeClass : idleClass)}
          >
            <Icon className="size-4" />
            {entry.label}
          </NavLink>
        )
      })}
    </nav>
  )
}
