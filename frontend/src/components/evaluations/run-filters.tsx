import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import type { RunFilters } from '@/hooks/use-evaluation-explorer'
import { batchKindLabel, DEMAND_PROFILE_LABELS, DEMAND_PROFILES, SCENARIO_KIND_LABELS } from '@/lib/explorer-labels'
import type { EvaluationBatch, EvaluationStatus, ScenarioKind } from '@/types/api'

const ALL = 'ALL'

export type FilterValues = Omit<RunFilters, 'page' | 'size'>

interface Props {
  value: FilterValues
  batches: EvaluationBatch[]
  onChange: (v: FilterValues) => void
}

function intOrUndefined(text: string): number | undefined {
  const n = Number(text)
  return text.trim() !== '' && Number.isInteger(n) ? n : undefined
}

/** Filters of the run list: kind, batch, profile, points, seed and status. */
export function RunFilterBar({ value, batches, onChange }: Props) {
  const set = (patch: Partial<FilterValues>) => onChange({ ...value, ...patch })
  return (
    <div className="flex flex-wrap items-end gap-3">
      <div className="flex flex-col gap-1">
        <Label htmlFor="run-kind">Kind</Label>
        <Select value={value.kind ?? ALL} onValueChange={(v) => set({ kind: v === ALL ? undefined : (v as ScenarioKind) })}>
          <SelectTrigger id="run-kind" className="w-40"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All</SelectItem>
            {(Object.keys(SCENARIO_KIND_LABELS) as ScenarioKind[]).map((k) => (
              <SelectItem key={k} value={k}>{SCENARIO_KIND_LABELS[k]}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="run-batch">Batch</Label>
        <Select value={value.batchId ?? ALL} onValueChange={(v) => set({ batchId: v === ALL ? undefined : v })}>
          <SelectTrigger id="run-batch" className="w-56"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All</SelectItem>
            {batches.map((b) => (
              <SelectItem key={b.id} value={b.id}>
                {batchKindLabel(b)}: {b.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="run-profile">Profile</Label>
        <Select value={value.profile ?? ALL} onValueChange={(v) => set({ profile: v === ALL ? undefined : v })}>
          <SelectTrigger id="run-profile" className="w-52"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All</SelectItem>
            {DEMAND_PROFILES.map((p) => (
              <SelectItem key={p} value={p}>{DEMAND_PROFILE_LABELS[p]}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="run-points">Points</Label>
        <Input id="run-points" className="w-24" inputMode="numeric" value={value.pointCount ?? ''} onChange={(e) => set({ pointCount: intOrUndefined(e.target.value) })} />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="run-seed">Seed</Label>
        <Input id="run-seed" className="w-24" inputMode="numeric" value={value.seed ?? ''} onChange={(e) => set({ seed: intOrUndefined(e.target.value) })} />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="run-status">Status</Label>
        <Select value={value.status ?? ALL} onValueChange={(v) => set({ status: v === ALL ? undefined : (v as EvaluationStatus) })}>
          <SelectTrigger id="run-status" className="w-40"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL}>All</SelectItem>
            <SelectItem value="COMPLETED">Completed</SelectItem>
            <SelectItem value="ROUTING_FAILED">Routing failed</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <Button variant="outline" onClick={() => onChange({})}>Clear filters</Button>
    </div>
  )
}
