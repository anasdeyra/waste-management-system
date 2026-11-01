import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api-client'
import type { EvaluationRun, EvaluationRunSummary } from '@/types/api'

export const evaluationsKey = ['evaluations'] as const
export const tourEvaluationsKey = (tourId: string) => ['evaluations', 'tour', tourId] as const
export const evaluationKey = (runId: string) => ['evaluations', 'run', runId] as const

/** Retained runs of a tour, newest first (GET /api/tours/{id}/evaluations). */
export function useTourEvaluations(tourId: string | undefined) {
  return useQuery({
    queryKey: tourEvaluationsKey(tourId ?? ''),
    queryFn: () => apiFetch<EvaluationRunSummary[]>(`/api/tours/${tourId}/evaluations`),
    enabled: !!tourId,
  })
}

/** One retained run with its three method results (GET /api/evaluations/{runId}). */
export function useEvaluation(runId: string | undefined) {
  return useQuery({
    queryKey: evaluationKey(runId ?? ''),
    queryFn: () => apiFetch<EvaluationRun>(`/api/evaluations/${runId}`),
    enabled: !!runId,
  })
}

/** Runs the three-method evaluation (POST /api/tours/{id}/evaluations, 201). */
export function useRunEvaluation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (v: { tourId: string }) =>
      apiFetch<EvaluationRun>(`/api/tours/${v.tourId}/evaluations`, { method: 'POST' }),
    onSuccess: (run, v) => {
      queryClient.setQueryData(evaluationKey(run.id), run)
      return queryClient.invalidateQueries({ queryKey: tourEvaluationsKey(v.tourId) })
    },
  })
}
