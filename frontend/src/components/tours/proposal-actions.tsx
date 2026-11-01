import { toast } from 'sonner'

import { Button } from '@/components/ui/button'
import { useAcceptProposal, useRejectProposal } from '@/hooks/use-tours'
import { STATUS_LABELS } from '@/lib/tour-labels'
import type { EvaluationRun, Tour } from '@/types/api'

/** Accept the NN + 2-opt proposal or keep the input order of a retained run (US-24). */
export function ProposalActions({ run, tour }: { run: EvaluationRun; tour: Tour }) {
  const accept = useAcceptProposal()
  const reject = useRejectProposal()
  const nn2opt = run.results.find((r) => r.method === 'NN_PLUS_2_OPT')
  const fresh = run.tourRevision === tour.revision
  const decidable =
    tour.status === 'DRAFT' &&
    run.status === 'COMPLETED' &&
    fresh &&
    !run.decision &&
    !!nn2opt?.feasible
  const pending = accept.isPending || reject.isPending
  const body = { evaluationRunId: run.id }
  const onError = (e: Error) => toast.error(e.message)

  let reason: string | null = null
  if (tour.status !== 'DRAFT') {
    reason = `The tour is ${STATUS_LABELS[tour.status]}; proposals can only be applied to a draft.`
  } else if (run.status === 'ROUTING_FAILED') {
    reason = 'No proposal is available for a failed run.'
  } else if (run.decision) {
    reason = 'Decision already recorded for this run.'
  } else if (!fresh) {
    reason = 'This run is stale: evaluate the tour again before deciding.'
  } else if (!nn2opt?.feasible) {
    reason = 'The NN + 2-opt result is infeasible.'
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap gap-2">
        <Button
          disabled={!decidable || pending}
          onClick={() =>
            accept.mutate(
              { id: tour.id, body },
              {
                onSuccess: () =>
                  toast.success(
                    'Proposal accepted: the stops now follow the NN + 2-opt order. The tour is still a draft.',
                  ),
                onError,
              },
            )
          }
        >
          Accept NN + 2-opt proposal
        </Button>
        <Button
          variant="outline"
          disabled={!decidable || pending}
          onClick={() =>
            reject.mutate(
              { id: tour.id, body },
              {
                onSuccess: () =>
                  toast.success(
                    'Input order kept with its road route. The proposal stays retained in the run.',
                  ),
                onError,
              },
            )
          }
        >
          Keep input order (reject proposal)
        </Button>
      </div>
      {!decidable && reason && <p className="text-sm text-muted-foreground">{reason}</p>}
      <p className="text-xs text-muted-foreground">
        Accepting reorders the draft's stops and attaches the proposal's road route. Keeping the input
        order attaches the input-order road route. Either way the tour stays a draft until you confirm
        it, and all three results stay retained.
      </p>
    </div>
  )
}
