import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch, apiPostCsv } from '@/lib/api-client'
import { evaluationsKey } from '@/hooks/use-evaluations'
import type {
  EvaluationBatch,
  EvaluationBatchDetail,
  EvaluationRunPage,
  EvaluationStatus,
  ExperimentRequest,
  ScenarioKind,
} from '@/types/api'

export interface RunFilters {
  kind?: ScenarioKind
  batchId?: string
  profile?: string
  pointCount?: number
  seed?: number
  status?: EvaluationStatus
  page: number
  size: number
}

export function useEvaluationBatches() {
  return useQuery({
    queryKey: [...evaluationsKey, 'batches'],
    queryFn: () => apiFetch<EvaluationBatch[]>('/api/evaluations/batches'),
  })
}

export function useEvaluationBatch(id: string | undefined) {
  return useQuery({
    queryKey: [...evaluationsKey, 'batch', id ?? ''],
    queryFn: () => apiFetch<EvaluationBatchDetail>(`/api/evaluations/batches/${id}`),
    enabled: !!id,
  })
}

export function useEvaluationRuns(filters: RunFilters) {
  const q = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== '') q.set(key, String(value))
  }
  return useQuery({
    queryKey: [...evaluationsKey, 'list', filters],
    queryFn: () => apiFetch<EvaluationRunPage>(`/api/evaluations?${q.toString()}`),
  })
}

/** POST /api/evaluations/imports with the raw CSV; 400 carries body.rowErrors. */
export function useImportScenarioCsv() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (v: { csv: string; fileName: string; label?: string }) => {
      const q = new URLSearchParams({ fileName: v.fileName })
      if (v.label) q.set('label', v.label)
      return apiPostCsv<EvaluationBatchDetail>(`/api/evaluations/imports?${q.toString()}`, v.csv)
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: evaluationsKey }),
  })
}

/** POST /api/admin/experiments (ADMIN): runs a seeded batch synchronously. */
export function useRunExperiment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: ExperimentRequest) =>
      apiFetch<EvaluationBatch>('/api/admin/experiments', { method: 'POST', body }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: evaluationsKey }),
  })
}
