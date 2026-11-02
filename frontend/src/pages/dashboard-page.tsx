import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Bar, BarChart, LabelList, XAxis, YAxis } from 'recharts'

import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ChartContainer, type ChartConfig } from '@/components/ui/chart'
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
import { useOperationsDashboard } from '@/hooks/use-dashboard'
import { pointLink, resourceLink, tourStatusLink } from '@/lib/dashboard-links'
import { STATUS_LABELS } from '@/lib/tour-labels'
import type { TourStatus } from '@/types/api'

type PeriodKind = 'ALL' | 'TODAY' | 'NEXT7' | 'CUSTOM'

function isoDate(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

const STATUS_ORDER: TourStatus[] = ['DRAFT', 'PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']
const STATUS_COLORS: Record<TourStatus, string> = {
  DRAFT: 'var(--color-chart-1, #64748b)',
  PLANNED: 'var(--color-chart-2, #0284c7)',
  IN_PROGRESS: 'var(--color-chart-3, #d97706)',
  COMPLETED: 'var(--color-chart-4, #16a34a)',
  CANCELLED: 'var(--color-chart-5, #dc2626)',
}
const chartConfig: ChartConfig = { count: { label: 'Tours' } }

function CountLink({ to, label, value }: { to: string; label: string; value: number }) {
  return (
    <Link to={to} className="flex items-baseline justify-between gap-4 hover:underline">
      <span>{label}</span>
      <span className="font-semibold tabular-nums">{value}</span>
    </Link>
  )
}

/** Operational dashboard (US-35): recorded planning and execution data, not live tracking. */
export default function DashboardPage() {
  const [kind, setKind] = useState<PeriodKind>('ALL')
  const [customFrom, setCustomFrom] = useState('')
  const [customTo, setCustomTo] = useState('')
  const [today] = useState(() => new Date())
  let from: string | undefined
  let to: string | undefined
  if (kind === 'TODAY') {
    from = to = isoDate(today)
  } else if (kind === 'NEXT7') {
    from = isoDate(today)
    const end = new Date(today)
    end.setDate(end.getDate() + 6)
    to = isoDate(end)
  } else if (kind === 'CUSTOM') {
    from = customFrom || undefined
    to = customTo || undefined
  }
  const { data, error, isPending } = useOperationsDashboard({ from, to })

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-xl font-semibold">Operational dashboard</h1>
        <p className="text-sm text-muted-foreground">
          Recorded planning and execution data. This is not live vehicle tracking.
        </p>
      </div>
      <div className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1">
          <Label htmlFor="dashboard-period">Planning period</Label>
          <Select value={kind} onValueChange={(v) => setKind(v as PeriodKind)}>
            <SelectTrigger id="dashboard-period" className="w-44">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All dates</SelectItem>
              <SelectItem value="TODAY">Today</SelectItem>
              <SelectItem value="NEXT7">Next 7 days</SelectItem>
              <SelectItem value="CUSTOM">Custom</SelectItem>
            </SelectContent>
          </Select>
        </div>
        {kind === 'CUSTOM' && (
          <>
            <div className="flex flex-col gap-1">
              <Label htmlFor="dashboard-from">From</Label>
              <Input
                id="dashboard-from"
                type="date"
                value={customFrom}
                onChange={(e) => setCustomFrom(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1">
              <Label htmlFor="dashboard-to">To</Label>
              <Input
                id="dashboard-to"
                type="date"
                value={customTo}
                onChange={(e) => setCustomTo(e.target.value)}
              />
            </div>
          </>
        )}
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertTitle>Could not load the dashboard</AlertTitle>
          <AlertDescription>{error.message}</AlertDescription>
        </Alert>
      )}
      {isPending && !error && (
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-32" />
          <Skeleton className="h-32" />
          <Skeleton className="h-48" />
          <Skeleton className="h-48" />
        </div>
      )}
      {data && (
        <>
          <div className="grid gap-4 md:grid-cols-3">
            <Card>
              <CardHeader>
                <CardTitle>Vehicles</CardTitle>
                <CardDescription>{data.vehicles.total} in total</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-1 text-sm">
                <CountLink to={resourceLink('vehicles', 'AVAILABLE')} label="Available" value={data.vehicles.available} />
                <CountLink to={resourceLink('vehicles', 'UNAVAILABLE')} label="Unavailable" value={data.vehicles.unavailable} />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Agents</CardTitle>
                <CardDescription>{data.agents.total} in total</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-1 text-sm">
                <CountLink to={resourceLink('agents', 'AVAILABLE')} label="Available" value={data.agents.available} />
                <CountLink to={resourceLink('agents', 'UNAVAILABLE')} label="Unavailable" value={data.agents.unavailable} />
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Collection points</CardTitle>
                <CardDescription>{data.collectionPoints.total} in total</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-1 text-sm">
                <CountLink to={pointLink('ACTIVE')} label="Active" value={data.collectionPoints.active} />
                <CountLink to={pointLink('INACTIVE')} label="Inactive" value={data.collectionPoints.inactive} />
              </CardContent>
            </Card>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle>Tours by status</CardTitle>
                <CardDescription>{data.tours.total} tours planned in the period</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-3">
                <div className="flex flex-col gap-1 text-sm">
                  {STATUS_ORDER.map((s) => (
                    <CountLink
                      key={s}
                      to={tourStatusLink(s, from, to)}
                      label={STATUS_LABELS[s]}
                      value={
                        {
                          DRAFT: data.tours.draft,
                          PLANNED: data.tours.planned,
                          IN_PROGRESS: data.tours.inProgress,
                          COMPLETED: data.tours.completed,
                          CANCELLED: data.tours.cancelled,
                        }[s]
                      }
                    />
                  ))}
                </div>
                <ChartContainer config={chartConfig} className="h-44 w-full">
                  <BarChart
                    layout="vertical"
                    data={STATUS_ORDER.map((s) => ({
                      status: STATUS_LABELS[s],
                      count: {
                        DRAFT: data.tours.draft,
                        PLANNED: data.tours.planned,
                        IN_PROGRESS: data.tours.inProgress,
                        COMPLETED: data.tours.completed,
                        CANCELLED: data.tours.cancelled,
                      }[s],
                      fill: STATUS_COLORS[s],
                    }))}
                    margin={{ right: 24 }}
                  >
                    <YAxis dataKey="status" type="category" width={90} tickLine={false} axisLine={false} />
                    <XAxis type="number" hide />
                    <Bar dataKey="count" radius={3}>
                      <LabelList dataKey="count" position="right" />
                    </Bar>
                  </BarChart>
                </ChartContainer>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Stop progress</CardTitle>
                <CardDescription>{data.stops.total} stops</CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col gap-3 text-sm">
                <div className="flex flex-col gap-1">
                  <div className="flex justify-between">
                    <span>Pending</span>
                    <span className="font-semibold tabular-nums">{data.stops.pending}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Collected</span>
                    <span className="font-semibold tabular-nums">{data.stops.collected}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Not collected</span>
                    <span className="font-semibold tabular-nums">{data.stops.notCollected}</span>
                  </div>
                </div>
                <div className="flex h-3 w-full overflow-hidden rounded bg-muted" aria-hidden="true">
                  {data.stops.total > 0 && (
                    <>
                      <div className="bg-slate-400" style={{ width: `${(data.stops.pending / data.stops.total) * 100}%` }} />
                      <div className="bg-green-600" style={{ width: `${(data.stops.collected / data.stops.total) * 100}%` }} />
                      <div className="bg-red-600" style={{ width: `${(data.stops.notCollected / data.stops.total) * 100}%` }} />
                    </>
                  )}
                </div>
                <div className="flex justify-between">
                  <span>Collected quantity</span>
                  <span className="font-semibold tabular-nums">{data.collectedKg.toFixed(1)} kg</span>
                </div>
                <p className="text-xs text-muted-foreground">Stops of draft and cancelled tours are not counted.</p>
                <p className="text-xs text-muted-foreground">Stop counts have no list view to link to.</p>
              </CardContent>
            </Card>
          </div>
          <p className="text-xs text-muted-foreground">
            Counts as recorded at {new Date(data.generatedAt).toLocaleString()}.
          </p>
        </>
      )}
    </div>
  )
}
