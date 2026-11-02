import { Plus } from 'lucide-react'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'

import { AgentFormDialog } from '@/components/agents/agent-form-dialog'
import { AgentLinkDialog } from '@/components/agents/agent-link-dialog'
import { originLabel } from '@/components/maps/geo'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Empty, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useAgents, useSetAgentStatus, useUnlinkAgentAccount } from '@/hooks/use-agents'
import { useCurrentUser } from '@/hooks/use-auth'
import { hasRole } from '@/lib/auth'
import { enumParam } from '@/lib/url-filters'
import type { Agent, ResourceStatus } from '@/types/api'

const ALL = 'ALL'

/** Operational agents: availability, editing, and (ADMIN) the login-account link. */
export default function AgentsPage() {
  const { data: user } = useCurrentUser()
  const isAdmin = hasRole(user, 'ADMIN')
  const [params] = useSearchParams()
  const [filter, setFilter] = useState<ResourceStatus | undefined>(() =>
    enumParam(params.get('status'), ['AVAILABLE', 'UNAVAILABLE'] as const),
  )
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Agent | undefined>(undefined)
  const [linking, setLinking] = useState<Agent | null>(null)
  const agents = useAgents(filter)
  const setStatus = useSetAgentStatus()
  const unlink = useUnlinkAgentAccount()

  const openForm = (a?: Agent) => {
    setEditing(a)
    setFormOpen(true)
  }

  const confirmUnlink = (a: Agent) => {
    if (!window.confirm(`Unlink ${a.linkedUser?.username} from ${a.displayName}?`)) return
    unlink.mutate(a.id, {
      onSuccess: () => toast.success('Account unlinked'),
      onError: (err) => toast.error(err.message),
    })
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">Agents ({agents.data?.length ?? 0})</h1>
        <div className="flex items-end gap-3">
          <div className="flex flex-col gap-1">
            <Label htmlFor="agent-filter">Availability</Label>
            <Select
              value={filter ?? ALL}
              onValueChange={(v) => setFilter(v === ALL ? undefined : (v as ResourceStatus))}
            >
              <SelectTrigger id="agent-filter" className="w-40">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>All</SelectItem>
                <SelectItem value="AVAILABLE">Available</SelectItem>
                <SelectItem value="UNAVAILABLE">Unavailable</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <Button onClick={() => openForm()}>
            <Plus />
            New agent
          </Button>
        </div>
      </div>

      {agents.isPending && (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      )}
      {agents.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load agents</AlertTitle>
          <AlertDescription>{agents.error.message}</AlertDescription>
        </Alert>
      )}
      {agents.data && agents.data.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>No agents match this filter</EmptyTitle>
          </EmptyHeader>
        </Empty>
      )}
      {agents.data && agents.data.length > 0 && (
        <div className="rounded-lg border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Display name</TableHead>
                <TableHead>Origin</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Available</TableHead>
                <TableHead>Login account</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {agents.data.map((a) => (
                <TableRow key={a.id}>
                  <TableCell className="font-medium">{a.displayName}</TableCell>
                  <TableCell>{originLabel(a.origin)}</TableCell>
                  <TableCell>
                    <Badge variant={a.status === 'AVAILABLE' ? 'default' : 'secondary'}>
                      {a.status === 'AVAILABLE' ? 'Available' : 'Unavailable'}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Switch
                      aria-label={`Available: ${a.displayName}`}
                      checked={a.status === 'AVAILABLE'}
                      disabled={setStatus.isPending}
                      onCheckedChange={(checked) =>
                        setStatus.mutate(
                          { id: a.id, status: checked ? 'AVAILABLE' : 'UNAVAILABLE' },
                          { onError: (err) => toast.error(err.message) },
                        )
                      }
                    />
                  </TableCell>
                  <TableCell>
                    {a.linkedUser ? (
                      <span className="flex items-center gap-2">
                        {a.linkedUser.username}
                        {!a.linkedUser.active && <Badge variant="secondary">inactive</Badge>}
                      </span>
                    ) : (
                      '—'
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      <Button variant="outline" size="sm" onClick={() => openForm(a)}>
                        Edit
                      </Button>
                      {isAdmin &&
                        (a.linkedUser ? (
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={unlink.isPending}
                            onClick={() => confirmUnlink(a)}
                          >
                            Unlink
                          </Button>
                        ) : (
                          <Button variant="outline" size="sm" onClick={() => setLinking(a)}>
                            Link account
                          </Button>
                        ))}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <AgentFormDialog open={formOpen} agent={editing} onOpenChange={setFormOpen} />
      <AgentLinkDialog
        agent={linking}
        open={linking !== null}
        onOpenChange={(o) => {
          if (!o) setLinking(null)
        }}
      />
    </div>
  )
}
