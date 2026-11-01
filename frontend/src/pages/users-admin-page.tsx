import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus } from 'lucide-react'
import { useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Empty, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
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
import { useAgents } from '@/hooks/use-agents'
import { useCurrentUser } from '@/hooks/use-auth'
import { apiFetch, ApiRequestError } from '@/lib/api-client'
import type { ActiveStatusRequest, CreateUserRequest, UserSummary } from '@/types/api'

const schema = z.object({
  username: z
    .string()
    .min(3, 'At least 3 characters')
    .max(50, 'At most 50 characters')
    .regex(/^[A-Za-z0-9._-]+$/, "Only letters, digits, '.', '_' and '-'"),
  password: z.string().min(8, 'At least 8 characters').max(72, 'At most 72 characters'),
  role: z.enum(['ADMIN', 'PLANNER', 'AGENT']),
  agentId: z.string().optional(),
})
type FormValues = z.infer<typeof schema>

const usersKey = ['users'] as const
const NONE = 'NONE'

/** Administrator page: list accounts, create accounts, activate or deactivate them. */
export default function UsersAdminPage() {
  const queryClient = useQueryClient()
  const { data: me } = useCurrentUser()
  const [open, setOpen] = useState(false)
  const agents = useAgents()
  const freeAgents = (agents.data ?? []).filter((a) => !a.linkedUser)
  const users = useQuery({
    queryKey: usersKey,
    queryFn: () => apiFetch<UserSummary[]>('/api/users'),
  })

  const toggle = useMutation({
    mutationFn: (v: { id: string; active: boolean }) =>
      apiFetch<UserSummary>(`/api/users/${v.id}/status`, {
        method: 'PATCH',
        body: { active: v.active } satisfies ActiveStatusRequest,
      }),
    onSuccess: (u) => {
      toast.success(`${u.username} is now ${u.active ? 'active' : 'inactive'}`)
      void queryClient.invalidateQueries({ queryKey: usersKey })
    },
    onError: (err: Error) => toast.error(err.message),
  })

  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', password: '', role: 'AGENT', agentId: NONE },
  })
  const selectedRole = useWatch({ control, name: 'role' })

  const create = useMutation({
    mutationFn: (v: CreateUserRequest) =>
      apiFetch<UserSummary>('/api/users', { method: 'POST', body: v }),
    onSuccess: () => {
      toast.success('User created')
      setOpen(false)
      reset()
      void queryClient.invalidateQueries({ queryKey: usersKey })
      void queryClient.invalidateQueries({ queryKey: ['agents'] })
    },
    onError: (err: Error) => {
      if (err instanceof ApiRequestError && err.status === 409 && /username/i.test(err.message)) {
        setError('username', { message: err.message })
      } else if (err instanceof ApiRequestError && err.status === 400 && err.body?.fieldErrors) {
        for (const fe of err.body.fieldErrors) {
          setError(fe.field as keyof FormValues, { message: fe.message })
        }
      } else {
        toast.error(err.message)
      }
    },
  })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-xl font-semibold">Users</h1>
        <Button onClick={() => setOpen(true)}>
          <Plus />
          New user
        </Button>
      </div>

      {users.isPending && (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      )}
      {users.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load users</AlertTitle>
          <AlertDescription>{users.error.message}</AlertDescription>
        </Alert>
      )}
      {users.data && users.data.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>No users yet</EmptyTitle>
          </EmptyHeader>
        </Empty>
      )}
      {users.data && users.data.length > 0 && (
        <div className="rounded-lg border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Username</TableHead>
                <TableHead>Role</TableHead>
                <TableHead>Linked agent</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Access</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {users.data.map((u) => (
                <TableRow key={u.id}>
                  <TableCell className="font-medium">{u.username}</TableCell>
                  <TableCell>
                    <Badge variant="outline">{u.role}</Badge>
                  </TableCell>
                  <TableCell>{u.linkedAgentName ?? '—'}</TableCell>
                  <TableCell>
                    <Badge variant={u.active ? 'default' : 'secondary'}>
                      {u.active ? 'Active' : 'Inactive'}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Switch
                      aria-label="Active"
                      checked={u.active}
                      disabled={u.username === me?.username || toggle.isPending}
                      onCheckedChange={(checked) => toggle.mutate({ id: u.id, active: checked })}
                    />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog
        open={open}
        onOpenChange={(o) => {
          setOpen(o)
          if (!o) reset()
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>New user</DialogTitle>
            <DialogDescription>
              The account can sign in immediately. Share the password securely.
            </DialogDescription>
          </DialogHeader>
          <form
            id="create-user-form"
            onSubmit={handleSubmit((v) =>
              create.mutate({
                username: v.username,
                password: v.password,
                role: v.role,
                agentId: v.role === 'AGENT' && v.agentId && v.agentId !== NONE ? v.agentId : undefined,
              }),
            )}
            className="flex flex-col gap-4"
            noValidate
          >
            <div className="flex flex-col gap-2">
              <Label htmlFor="new-username">Username</Label>
              <Input id="new-username" autoComplete="off" {...register('username')} />
              {errors.username && (
                <p className="text-sm text-destructive">{errors.username.message}</p>
              )}
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="new-password">Password</Label>
              <Input
                id="new-password"
                type="password"
                autoComplete="new-password"
                {...register('password')}
              />
              {errors.password && (
                <p className="text-sm text-destructive">{errors.password.message}</p>
              )}
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="new-role">Role</Label>
              <Controller
                control={control}
                name="role"
                render={({ field }) => (
                  <Select value={field.value} onValueChange={field.onChange}>
                    <SelectTrigger id="new-role" className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ADMIN">ADMIN</SelectItem>
                      <SelectItem value="PLANNER">PLANNER</SelectItem>
                      <SelectItem value="AGENT">AGENT</SelectItem>
                    </SelectContent>
                  </Select>
                )}
              />
              {errors.role && <p className="text-sm text-destructive">{errors.role.message}</p>}
            </div>
            {selectedRole === 'AGENT' && (
              <div className="flex flex-col gap-2">
                <Label htmlFor="new-agent">Link to agent (optional)</Label>
                <Controller
                  control={control}
                  name="agentId"
                  render={({ field }) => (
                    <Select value={field.value ?? NONE} onValueChange={field.onChange}>
                      <SelectTrigger id="new-agent" className="w-full">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value={NONE}>None</SelectItem>
                        {freeAgents.map((a) => (
                          <SelectItem key={a.id} value={a.id}>
                            {a.displayName}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>
            )}
          </form>
          <DialogFooter>
            <Button type="submit" form="create-user-form" disabled={create.isPending}>
              Create user
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
