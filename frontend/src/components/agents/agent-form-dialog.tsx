import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { toast } from 'sonner'
import { z } from 'zod'

import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useCreateAgent, useUpdateAgent } from '@/hooks/use-agents'
import { ApiRequestError } from '@/lib/api-client'
import type { Agent } from '@/types/api'

const schema = z.object({
  displayName: z.string().trim().min(1, 'Name is required').max(100, 'At most 100 characters'),
})
type FormValues = z.infer<typeof schema>

/** Create or edit an operational agent. Display names need not be unique. */
export function AgentFormDialog({
  open,
  agent,
  onOpenChange,
}: {
  open: boolean
  agent?: Agent
  onOpenChange: (open: boolean) => void
}) {
  const create = useCreateAgent()
  const update = useUpdateAgent()
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { displayName: '' },
  })

  useEffect(() => {
    if (open) reset({ displayName: agent?.displayName ?? '' })
  }, [open, agent, reset])

  const onError = (err: Error) => {
    if (err instanceof ApiRequestError && err.status === 400 && err.body?.fieldErrors) {
      for (const fe of err.body.fieldErrors) {
        setError(fe.field as keyof FormValues, { message: fe.message })
      }
    } else {
      toast.error(err.message)
    }
  }

  const onSubmit = (v: FormValues) => {
    const body = { displayName: v.displayName }
    const done = () => {
      toast.success(agent ? 'Agent updated' : 'Agent created')
      onOpenChange(false)
    }
    if (agent) update.mutate({ id: agent.id, body }, { onSuccess: done, onError })
    else create.mutate(body, { onSuccess: done, onError })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{agent ? 'Edit agent' : 'New agent'}</DialogTitle>
          <DialogDescription>
            An agent is an operational person. A login account can be linked by an administrator.
          </DialogDescription>
        </DialogHeader>
        <form
          id="agent-form"
          onSubmit={handleSubmit(onSubmit)}
          className="flex flex-col gap-4"
          noValidate
        >
          <div className="flex flex-col gap-2">
            <Label htmlFor="agent-name">Display name</Label>
            <Input id="agent-name" autoComplete="off" {...register('displayName')} />
            {errors.displayName && (
              <p className="text-sm text-destructive">{errors.displayName.message}</p>
            )}
          </div>
        </form>
        <DialogFooter>
          <Button type="submit" form="agent-form" disabled={create.isPending || update.isPending}>
            {agent ? 'Save changes' : 'Create agent'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
