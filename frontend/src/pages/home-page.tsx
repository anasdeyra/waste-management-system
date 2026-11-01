import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useCurrentUser } from '@/hooks/use-auth'
import type { AppRole } from '@/types/api'

const roleText: Record<AppRole, string> = {
  ADMIN:
    'You can manage user accounts, waste categories, collection points, vehicles and agents, and run the MN01 data import.',
  PLANNER:
    'You can manage collection points, vehicles and agents. Tour planning arrives in a later sprint.',
  AGENT: 'Your assigned tours will appear here in a later sprint.',
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
