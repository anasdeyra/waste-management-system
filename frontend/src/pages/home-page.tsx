import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useCurrentUser } from '@/hooks/use-auth'
import type { AppRole } from '@/types/api'

const roleText: Record<AppRole, string> = {
  ADMIN:
    'You can manage user accounts, waste categories, collection points, vehicles and agents, plan tour drafts, run the MN01 data import and load the demonstration data. You can cancel tours with a reason, correct recorded outcomes and search the operational history.',
  PLANNER:
    'You can manage collection points, vehicles and agents, plan and evaluate tour drafts, confirm them as planned, and follow their execution. You can cancel tours with a reason, correct recorded outcomes and search the operational history.',
  AGENT:
    'Open My tours to consult your assigned tours, start a planned tour, record each stop outcome and complete the tour.',
}

/** Landing page after sign-in. */
export default function HomePage() {
  const { data: user } = useCurrentUser()
  if (!user) return null
  return (
    <Card>
      <CardHeader>
        <CardTitle>Welcome, {user.username}</CardTitle>
        <CardDescription>
          Signed in as <Badge variant="secondary">{user.role}</Badge>
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm">{roleText[user.role]}</p>
      </CardContent>
    </Card>
  )
}
