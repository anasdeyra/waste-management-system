import type { PlanningMethod, ProposalDecision } from '@/types/api'

export const METHOD_ORDER: PlanningMethod[] = ['INPUT_ORDER', 'NEAREST_NEIGHBOR', 'NN_PLUS_2_OPT']

export const METHOD_LABELS: Record<PlanningMethod, string> = {
  INPUT_ORDER: 'Input order (baseline)',
  NEAREST_NEIGHBOR: 'Nearest neighbor',
  NN_PLUS_2_OPT: 'NN + 2-opt (proposal)',
}

/** Distinct, stacked styles: wide/faint baseline under narrower, stronger lines. */
export const METHOD_STYLES: Record<
  PlanningMethod,
  { color: string; weight: number; opacity: number }
> = {
  INPUT_ORDER: { color: '#d97706', weight: 9, opacity: 0.45 },
  NEAREST_NEIGHBOR: { color: '#0284c7', weight: 6, opacity: 0.7 },
  NN_PLUS_2_OPT: { color: '#16a34a', weight: 3, opacity: 1 },
}

export const DECISION_LABELS: Record<ProposalDecision, string> = {
  ACCEPTED: 'Proposal accepted',
  REJECTED: 'Input order kept (proposal rejected)',
}
