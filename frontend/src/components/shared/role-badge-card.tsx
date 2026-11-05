import { useState } from 'react'

import { Badge } from '@/components/ui/badge'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import type { AppRole, CurrentUser } from '@/types/api'

/** What each role can do, shown when the user hovers over their role badge. */
const roleCapabilities: Record<AppRole, string[]> = {
  ADMIN: [
    'Manage user accounts, waste categories, collection points, vehicles and agents.',
    'Plan tour drafts, cancel tours with a reason, correct recorded outcomes and search the operational history.',
    'Run the MN01 data import and load the demonstration data.',
    'Review the audit log, the operational dashboard and the evaluation explorer (CSV import and export of evaluation scenarios and results, seeded experiment batches).',
    'Switch on the display-only NYC DOT traffic layer on the maps.',
  ],
  PLANNER: [
    'Manage collection points, vehicles and agents.',
    'Plan and evaluate tour drafts, confirm them as planned, and follow their execution.',
    'Cancel tours with a reason, correct recorded outcomes and search the operational history.',
    'Open the operational dashboard and the evaluation explorer (scenario CSV import, results export).',
    'Switch on the display-only NYC DOT traffic layer on the maps.',
  ],
  AGENT: [
    'Open My tours to consult your assigned tours.',
    'Start a planned tour, record each stop outcome and complete the tour.',
  ],
}

/**
 * The signed-in user's role badge. Hovering or focusing it opens a card listing what the role
 * can do; a tap opens it too, because Radix hover cards ignore touch input.
 */
export function RoleBadgeCard({ user }: { user: CurrentUser }) {
  const [open, setOpen] = useState(false)

  return (
    <HoverCard open={open} onOpenChange={setOpen} openDelay={150} closeDelay={100}>
      <HoverCardTrigger asChild>
        <button
          type="button"
          className="rounded-4xl outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
          aria-label={`Role ${user.role}: what you can do`}
          onTouchEnd={() => setOpen(true)}
        >
          <Badge variant="secondary" className="cursor-help">
            {user.role}
          </Badge>
        </button>
      </HoverCardTrigger>
      <HoverCardContent align="end" className="w-80 max-w-[calc(100vw-2rem)]">
        <p className="text-sm font-medium">Welcome, {user.username}</p>
        <p className="mb-2 text-xs text-muted-foreground">
          Signed in as {user.role}. With this role you can:
        </p>
        <ul className="flex list-disc flex-col gap-1.5 pl-4 text-xs">
          {roleCapabilities[user.role].map((capability) => (
            <li key={capability}>{capability}</li>
          ))}
        </ul>
      </HoverCardContent>
    </HoverCard>
  )
}
