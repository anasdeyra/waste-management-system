import { useState } from 'react'
import { Bar, BarChart, CartesianGrid, LabelList, XAxis, YAxis } from 'recharts'
import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { apiDownload } from '@/lib/api-client'
import { formatDistanceKm, formatExecutionMs, formatInstant } from '@/lib/evaluation-format'
import { METHOD_LABELS, METHOD_STYLES } from '@/lib/evaluation-labels'
import { batchKindLabel, formatChangePct } from '@/lib/explorer-labels'
import type { EvaluationBatchDetail } from '@/types/api'

type Group = 'ALL' | 'SIZE' | 'PROFILE'

const chartConfig: ChartConfig = {
  NEAREST_NEIGHBOR: { label: METHOD_LABELS.NEAREST_NEIGHBOR, color: METHOD_STYLES.NEAREST_NEIGHBOR.color },
  NN_PLUS_2_OPT: { label: METHOD_LABELS.NN_PLUS_2_OPT, color: METHOD_STYLES.NN_PLUS_2_OPT.color },
}

function Item({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="break-words text-sm">{children ?? '—'}</dd>
    </div>
  )
}

function download(path: string, fallback: string) {
  apiDownload(path, fallback).catch((e: Error) => toast.error(e.message))
}

/** Reproducibility fields, per-group aggregates, size chart and exports of one batch. */
export function BatchSummary({ detail }: { detail: EvaluationBatchDetail }) {
  const { batch, aggregates, scenarioKeys } = detail
  const [group, setGroup] = useState<Group>('ALL')
  const rows = aggregates.filter((a) => a.group === group)
  const sizes = aggregates.filter((a) => a.group === 'SIZE')
  const chartData = [...new Set(sizes.map((a) => a.value))].map((size) => ({
    size,
    NEAREST_NEIGHBOR: sizes.find((a) => a.value === size && a.method === 'NEAREST_NEIGHBOR')?.meanDistanceChangePct,
    NN_PLUS_2_OPT: sizes.find((a) => a.value === size && a.method === 'NN_PLUS_2_OPT')?.meanDistanceChangePct,
  }))
  return (
    <Card>
      <CardHeader>
        <CardTitle>{batch.label}</CardTitle>
        <CardDescription>
          {batchKindLabel(batch)} · {batch.completedRunCount} completed, {batch.failedRunCount} routing failed
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <section>
          <h3 className="mb-2 text-sm font-semibold">Reproducibility</h3>
          <dl className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <Item label="Parameters">{batch.parameters}</Item>
            <Item label="Profiles">{batch.demandProfiles.join(', ') || undefined}</Item>
            <Item label="Seeds">{batch.seeds.join(', ') || undefined}</Item>
            <Item label="Sizes">{batch.sizes.join(', ') || undefined}</Item>
            <Item label="Repetitions">{batch.repetitions}</Item>
            <Item label="Vehicle capacity">
              {batch.vehicleCapacityKg === undefined ? undefined : `${batch.vehicleCapacityKg} kg`}
            </Item>
            <Item label="Source snapshots">{batch.sourceSnapshotIds.join('; ') || undefined}</Item>
            <Item label="Road graph version">{batch.roadGraphVersion}</Item>
            <Item label="Routing engine">{batch.routingEngine}</Item>
            <Item label="Environment">{batch.environment}</Item>
            {batch.fileName && <Item label="File name">{batch.fileName}</Item>}
            {batch.fileSha256 && <Item label="File SHA-256">{batch.fileSha256}</Item>}
            <Item label="Created">
              {batch.createdBy}, {formatInstant(batch.createdAt)}
            </Item>
            <Item label="Finished">{batch.finishedAt ? formatInstant(batch.finishedAt) : undefined}</Item>
          </dl>
        </section>

        <section className="flex flex-col gap-2">
          <div className="flex flex-wrap items-end justify-between gap-2">
            <h3 className="text-sm font-semibold">Aggregates</h3>
            <Select value={group} onValueChange={(v) => setGroup(v as Group)}>
              <SelectTrigger className="w-40" aria-label="Group aggregates">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All runs</SelectItem>
                <SelectItem value="SIZE">By size</SelectItem>
                <SelectItem value="PROFILE">By profile</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  {group !== 'ALL' && <TableHead>{group === 'SIZE' ? 'Points' : 'Profile'}</TableHead>}
                  <TableHead>Method</TableHead>
                  <TableHead>Runs</TableHead>
                  <TableHead>Comparable</TableHead>
                  <TableHead>Excluded</TableHead>
                  <TableHead>Mean distance</TableHead>
                  <TableHead>Mean distance change</TableHead>
                  <TableHead>Min / max</TableHead>
                  <TableHead>Mean duration change</TableHead>
                  <TableHead>Mean execution time</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {rows.map((a) => (
                  <TableRow key={`${a.value}-${a.method}`}>
                    {group !== 'ALL' && <TableCell>{a.value}</TableCell>}
                    <TableCell>{METHOD_LABELS[a.method]}</TableCell>
                    <TableCell>{a.runs}</TableCell>
                    <TableCell>{a.comparableRuns}</TableCell>
                    <TableCell>{a.excludedRuns}</TableCell>
                    <TableCell>{formatDistanceKm(a.meanDistanceM)}</TableCell>
                    <TableCell>{formatChangePct(a.meanDistanceChangePct)}</TableCell>
                    <TableCell>
                      {formatChangePct(a.minDistanceChangePct)} / {formatChangePct(a.maxDistanceChangePct)}
                    </TableCell>
                    <TableCell>{formatChangePct(a.meanDurationChangePct)}</TableCell>
                    <TableCell>
                      {a.meanExecutionTimeMs === undefined ? '—' : formatExecutionMs(a.meanExecutionTimeMs)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <p className="text-xs text-muted-foreground">
            Change vs input order is shown only when both results are feasible.
          </p>
        </section>

        {chartData.length > 0 && (
          <section>
            <h3 className="mb-2 text-sm font-semibold">Mean distance change by size</h3>
            <ChartContainer config={chartConfig} className="h-64 w-full">
              <BarChart data={chartData} margin={{ top: 20 }}>
                <CartesianGrid vertical={false} />
                <XAxis dataKey="size" tickLine={false} axisLine={false} label={{ value: 'Points', position: 'insideBottom', offset: -4 }} />
                <YAxis tickFormatter={(v: number) => `${v}%`} width={50} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <ChartLegend content={<ChartLegendContent />} />
                <Bar dataKey="NEAREST_NEIGHBOR" fill={METHOD_STYLES.NEAREST_NEIGHBOR.color} radius={3}>
                  <LabelList dataKey="NEAREST_NEIGHBOR" position="top" formatter={(v: unknown) => formatChangePct(v as number | undefined)} />
                </Bar>
                <Bar dataKey="NN_PLUS_2_OPT" fill={METHOD_STYLES.NN_PLUS_2_OPT.color} radius={3}>
                  <LabelList dataKey="NN_PLUS_2_OPT" position="top" formatter={(v: unknown) => formatChangePct(v as number | undefined)} />
                </Bar>
              </BarChart>
            </ChartContainer>
          </section>
        )}

        <details>
          <summary className="cursor-pointer text-sm font-semibold">Scenario keys ({scenarioKeys.length})</summary>
          <ul className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-xs">
            {scenarioKeys.map((k) => (
              <li key={k}>{k}</li>
            ))}
          </ul>
        </details>

        <div className="flex flex-wrap gap-2">
          <Button
            variant="outline"
            onClick={() => download(`/api/evaluations/export?batchId=${batch.id}`, `evaluation-results-batch-${batch.id}.csv`)}
          >
            Export results CSV
          </Button>
          <Button
            variant="outline"
            onClick={() =>
              download(`/api/evaluations/batches/${batch.id}/scenarios.csv`, `evaluation-scenarios-${batch.id}.csv`)
            }
          >
            Download scenarios CSV
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
