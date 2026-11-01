import { useState } from 'react'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useAuditEvents } from '@/hooks/use-audit-events'
import type { AuditFilters } from '@/hooks/use-audit-events'
import {
  AUDIT_ACTION_LABELS,
  AUDIT_ACTIONS,
  AUDIT_ENTITY_TYPES,
  AUDIT_OUTCOMES,
  toUtcIso,
} from '@/lib/audit-labels'
import type { AuditAction, AuditEntityType, AuditOutcome } from '@/types/api'

const ALL = 'ALL'
const SIZE = 50

interface Draft {
  from: string
  to: string
  actor: string
  action: string
  entityType: string
  entityId: string
  outcome: string
}

const EMPTY: Draft = { from: '', to: '', actor: '', action: ALL, entityType: ALL, entityId: '', outcome: ALL }

function toFilters(d: Draft, page: number): AuditFilters {
  return {
    from: toUtcIso(d.from),
    to: toUtcIso(d.to),
    actor: d.actor.trim() || undefined,
    action: d.action === ALL ? undefined : (d.action as AuditAction),
    entityType: d.entityType === ALL ? undefined : (d.entityType as AuditEntityType),
    entityId: d.entityId.trim() || undefined,
    outcome: d.outcome === ALL ? undefined : (d.outcome as AuditOutcome),
    page,
    size: SIZE,
  }
}

/** Administrator audit log: read-only search of sign-ins and data changes (US-39). */
export default function AuditLogPage() {
  const [draft, setDraft] = useState<Draft>(EMPTY)
  const [applied, setApplied] = useState<AuditFilters>(toFilters(EMPTY, 0))
  const events = useAuditEvents(applied)
  const set = (patch: Partial<Draft>) => setDraft((d) => ({ ...d, ...patch }))
  const apply = () => setApplied(toFilters(draft, 0))
  const clear = () => {
    setDraft(EMPTY)
    setApplied(toFilters(EMPTY, 0))
  }
  const goto = (page: number) => setApplied((f) => ({ ...f, page }))
  const data = events.data

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-xl font-semibold">Audit log</h1>
        <p className="text-sm text-muted-foreground">
          Sign-ins and data changes with actor, time, action, entity and outcome. Audit events are read-only.
        </p>
      </div>
      <div className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-from">From</Label>
          <Input
            id="audit-from"
            type="datetime-local"
            className="w-52"
            value={draft.from}
            onChange={(e) => set({ from: e.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-to">To</Label>
          <Input
            id="audit-to"
            type="datetime-local"
            className="w-52"
            value={draft.to}
            onChange={(e) => set({ to: e.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-actor">Actor</Label>
          <Input
            id="audit-actor"
            className="w-40"
            value={draft.actor}
            onChange={(e) => set({ actor: e.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-action">Action</Label>
          <Select value={draft.action} onValueChange={(v) => set({ action: v })}>
            <SelectTrigger id="audit-action" className="w-56">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All actions</SelectItem>
              {AUDIT_ACTIONS.map((a) => (
                <SelectItem key={a} value={a}>
                  {AUDIT_ACTION_LABELS[a]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-entity-type">Entity type</Label>
          <Select value={draft.entityType} onValueChange={(v) => set({ entityType: v })}>
            <SelectTrigger id="audit-entity-type" className="w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All types</SelectItem>
              {AUDIT_ENTITY_TYPES.map((t) => (
                <SelectItem key={t} value={t}>
                  {t}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-entity-id">Entity id</Label>
          <Input
            id="audit-entity-id"
            className="w-72"
            value={draft.entityId}
            onChange={(e) => set({ entityId: e.target.value })}
          />
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor="audit-outcome">Outcome</Label>
          <Select value={draft.outcome} onValueChange={(v) => set({ outcome: v })}>
            <SelectTrigger id="audit-outcome" className="w-36">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>All</SelectItem>
              {AUDIT_OUTCOMES.map((o) => (
                <SelectItem key={o} value={o}>
                  {o === 'SUCCESS' ? 'Success' : 'Failure'}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <Button onClick={apply}>Apply</Button>
        <Button variant="outline" onClick={clear}>
          Clear filters
        </Button>
      </div>
      <p className="text-xs text-muted-foreground">Times are entered in your local time and stored in UTC.</p>

      {events.isPending && <Skeleton className="h-10 w-full" />}
      {events.isError && (
        <Alert variant="destructive">
          <AlertTitle>Could not load the audit log</AlertTitle>
          <AlertDescription>{events.error.message}</AlertDescription>
        </Alert>
      )}
      {data && data.items.length === 0 && (
        <Empty>
          <EmptyHeader>
            <EmptyTitle>No audit events match these filters.</EmptyTitle>
          </EmptyHeader>
        </Empty>
      )}
      {data && data.items.length > 0 && (
        <div className="rounded-lg border bg-background">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Time (UTC)</TableHead>
                <TableHead>Actor</TableHead>
                <TableHead>Action</TableHead>
                <TableHead>Entity</TableHead>
                <TableHead>Outcome</TableHead>
                <TableHead>Details</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data.items.map((e) => (
                <TableRow key={e.id}>
                  <TableCell title={new Date(e.occurredAt).toLocaleString()} className="whitespace-nowrap">
                    {e.occurredAt}
                  </TableCell>
                  <TableCell>{e.actor}</TableCell>
                  <TableCell>{AUDIT_ACTION_LABELS[e.action]}</TableCell>
                  <TableCell className="break-all">
                    {e.entityType ? `${e.entityType} ${e.entityId ?? ''}`.trim() : '—'}
                  </TableCell>
                  <TableCell>
                    <Badge variant={e.outcome === 'FAILURE' ? 'destructive' : 'secondary'}>
                      {e.outcome === 'FAILURE' ? 'Failure' : 'Success'}
                    </Badge>
                  </TableCell>
                  <TableCell>{e.details ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      {data && (
        <div className="flex items-center gap-3">
          <Button variant="outline" disabled={data.page <= 0} onClick={() => goto(data.page - 1)}>
            Previous
          </Button>
          <span className="text-sm">
            Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalElements} events
          </span>
          <Button
            variant="outline"
            disabled={data.page + 1 >= data.totalPages}
            onClick={() => goto(data.page + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  )
}
