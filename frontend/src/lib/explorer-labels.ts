import type { BatchStatus, DemandProfile, EvaluationBatch, ScenarioKind } from '@/types/api'

export const SCENARIO_KIND_LABELS: Record<ScenarioKind, string> = {
  TOUR_DRAFT: 'Tour evaluation',
  EXPERIMENT: 'Experiment',
  CSV_IMPORT: 'CSV import',
}

export const DEMAND_PROFILE_LABELS: Record<DemandProfile, string> = {
  UNIFORM_LOW: 'Uniform low (10–30 kg)',
  UNIFORM_HIGH: 'Uniform high (30–60 kg)',
  HOTSPOT: 'Hotspot (1 in 5 at 60–120 kg)',
}

export const DEMAND_PROFILES: DemandProfile[] = ['UNIFORM_LOW', 'UNIFORM_HIGH', 'HOTSPOT']

export const BATCH_STATUS_LABELS: Record<BatchStatus, string> = {
  RUNNING: 'Running',
  COMPLETED: 'Completed',
  ABORTED: 'Aborted',
}

export function batchKindLabel(batch: Pick<EvaluationBatch, 'kind' | 'canonical'>): string {
  if (batch.kind === 'CSV_IMPORT') return 'CSV import'
  return batch.canonical ? 'Fixed 60-scenario experiment' : 'Experiment subset'
}

/** Signed percentage with one decimal; an em dash when there is no comparable value. */
export function formatChangePct(v?: number): string {
  if (v === undefined) return '—'
  const text = Math.abs(v).toFixed(1)
  if (text === '0.0') return '0.0%'
  return `${v < 0 ? '-' : '+'}${text}%`
}

/** Comma-separated numbers, or null when any part is not a number. */
export function parseNumberList(text: string): number[] | null {
  const parts = text.split(',').map((p) => p.trim()).filter((p) => p !== '')
  if (parts.length === 0) return null
  const out: number[] = []
  for (const p of parts) {
    const n = Number(p)
    if (!Number.isInteger(n)) return null
    out.push(n)
  }
  return out
}
