import { useState } from 'react'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useRunExperiment } from '@/hooks/use-evaluation-explorer'
import { ApiRequestError } from '@/lib/api-client'
import { DEMAND_PROFILE_LABELS, DEMAND_PROFILES, parseNumberList } from '@/lib/explorer-labels'
import type { DemandProfile } from '@/types/api'

/** ADMIN only: runs a seeded experiment batch synchronously. */
export function ExperimentLauncher({ onDone }: { onDone: (batchId: string) => void }) {
  const [profiles, setProfiles] = useState<DemandProfile[]>([...DEMAND_PROFILES])
  const [seeds, setSeeds] = useState('1,2,3,4,5')
  const [sizes, setSizes] = useState('10,25,50,100')
  const [repetitions, setRepetitions] = useState('1')
  const [label, setLabel] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)
  const run = useRunExperiment()
  const fieldErrors = run.error instanceof ApiRequestError ? (run.error.body?.fieldErrors ?? []) : []
  const errorFor = (field: string) => fieldErrors.find((f) => f.field === field)?.message

  const submit = () => {
    const s = parseNumberList(seeds)
    const z = parseNumberList(sizes)
    if (!s || !z) {
      setLocalError('Seeds and sizes must be comma-separated whole numbers.')
      return
    }
    setLocalError(null)
    run.mutate(
      { profiles, seeds: s, sizes: z, repetitions: Number(repetitions), label: label.trim() || undefined },
      {
        onSuccess: (batch) => {
          toast.success('Experiment batch completed')
          onDone(batch.id)
        },
        onError: (e) => {
          if (!(e instanceof ApiRequestError) || !e.body?.fieldErrors) toast.error(e.message)
        },
      },
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Run experiment</CardTitle>
        <CardDescription>The full 60-scenario experiment takes about one to two minutes.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <fieldset className="flex flex-wrap gap-4">
          <legend className="mb-1 text-sm font-medium">Demand profiles</legend>
          {DEMAND_PROFILES.map((p) => (
            <label key={p} className="flex items-center gap-2 text-sm">
              <Checkbox
                checked={profiles.includes(p)}
                onCheckedChange={(c) => setProfiles((cur) => (c ? [...cur, p] : cur.filter((x) => x !== p)))}
              />
              {DEMAND_PROFILE_LABELS[p]}
            </label>
          ))}
        </fieldset>
        {errorFor('profiles') && <p className="text-sm text-destructive">{errorFor('profiles')}</p>}
        <div className="flex flex-wrap gap-3">
          <div className="flex flex-col gap-1">
            <Label htmlFor="exp-seeds">Seeds</Label>
            <Input id="exp-seeds" value={seeds} onChange={(e) => setSeeds(e.target.value)} />
            {errorFor('seeds') && <p className="text-sm text-destructive">{errorFor('seeds')}</p>}
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="exp-sizes">Sizes</Label>
            <Input id="exp-sizes" value={sizes} onChange={(e) => setSizes(e.target.value)} />
            {errorFor('sizes') && <p className="text-sm text-destructive">{errorFor('sizes')}</p>}
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="exp-repetitions">Repetitions</Label>
            <Input id="exp-repetitions" className="w-24" type="number" min={1} max={3} value={repetitions} onChange={(e) => setRepetitions(e.target.value)} />
            {errorFor('repetitions') && <p className="text-sm text-destructive">{errorFor('repetitions')}</p>}
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor="exp-label">Label</Label>
            <Input id="exp-label" value={label} onChange={(e) => setLabel(e.target.value)} maxLength={120} />
            {errorFor('label') && <p className="text-sm text-destructive">{errorFor('label')}</p>}
          </div>
        </div>
        {localError && <p className="text-sm text-destructive">{localError}</p>}
        <div>
          <Button onClick={submit} disabled={run.isPending}>
            {run.isPending ? 'Running…' : 'Run experiment'}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
