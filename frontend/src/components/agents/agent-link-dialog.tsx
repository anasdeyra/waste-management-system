import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { toast } from 'sonner'

import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useLinkAgentAccount } from '@/hooks/use-agents'
import { apiFetch } from '@/lib/api-client'
import type { Agent, UserSummary } from '@/types/api'

/** ADMIN dialog: pick a free AGENT account to link to the agent (one account per agent). */
export function AgentLinkDialog({
  agent,
  open,
  onOpenChange,
}: {
  agent: Agent | null
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const [userId, setUserId] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const link = useLinkAgentAccount()
  const users = useQuery({
    queryKey: ['users'],
    queryFn: () => apiFetch<UserSummary[]>('/api/users'),
    enabled: open,
  })
  const candidates = (users.data ?? []).filter((u) => u.role === 'AGENT' && !u.linkedAgentId)

  const close = (o: boolean) => {
    if (!o) {
      setUserId('')
      setMessage(null)
    }
    onOpenChange(o)
  }

  const submit = () => {
    if (!agent || !userId) return
    setMessage(null)
    link.mutate(
      { agentId: agent.id, userId },
      {
        onSuccess: () => {
          toast.success('Account linked')
          close(false)
        },
        onError: (err) => setMessage(err.message),
      },
    )
  }

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Link account to {agent?.displayName}</DialogTitle>
          <DialogDescription>
            Only AGENT accounts can be linked; one account per agent.
          </DialogDescription>
        </DialogHeader>
        {message && (
          <Alert variant="destructive">
            <AlertDescription>{message}</AlertDescription>
          </Alert>
        )}
        <div className="flex flex-col gap-2">
          <Label htmlFor="link-account">Account</Label>
          <Select value={userId} onValueChange={setUserId}>
            <SelectTrigger id="link-account" className="w-full">
              <SelectValue placeholder={users.isPending ? 'Loading…' : 'Select an account'} />
            </SelectTrigger>
            <SelectContent>
              {candidates.map((u) => (
                <SelectItem key={u.id} value={u.id}>
                  {u.username}
                  {u.active ? '' : ' (inactive)'}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {users.data && candidates.length === 0 && (
            <p className="text-sm text-muted-foreground">No unlinked AGENT accounts available.</p>
          )}
        </div>
        <DialogFooter>
          <Button onClick={submit} disabled={!userId || link.isPending}>
            Link account
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
