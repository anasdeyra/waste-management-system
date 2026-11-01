import { useState } from 'react'

import { BaseMap } from '@/components/maps/base-map'
import { EvaluationRouteLayers } from '@/components/maps/route-layers'
import { EvaluationIndicatorTable } from '@/components/tours/evaluation-indicator-table'
import { METHOD_LABELS, METHOD_ORDER, METHOD_STYLES } from '@/lib/evaluation-labels'
import type { EvaluationRun, PlanningMethod } from '@/types/api'

/** Map with one toggleable road route per method plus the indicator table of one run. */
export function EvaluationComparison({ run }: { run: EvaluationRun }) {
  const [visible, setVisible] = useState<Record<PlanningMethod, boolean>>({
    INPUT_ORDER: true,
    NEAREST_NEIGHBOR: true,
    NN_PLUS_2_OPT: true,
  })
  const proposalFeasible = run.results.some((r) => r.method === 'NN_PLUS_2_OPT' && r.feasible)
  const [numbersFor, setNumbersFor] = useState<PlanningMethod>(
    proposalFeasible ? 'NN_PLUS_2_OPT' : 'INPUT_ORDER',
  )
  return (
    <div className="flex flex-col gap-3">
      <BaseMap className="h-[460px]">
        <EvaluationRouteLayers run={run} visible={visible} numbersFor={numbersFor} />
      </BaseMap>
      <ul className="flex flex-wrap gap-4 text-xs text-muted-foreground">
        {METHOD_ORDER.map((m) => (
          <li key={m} className="flex items-center gap-1">
            <span
              className="inline-block h-1 w-6 rounded"
              style={{ backgroundColor: METHOD_STYLES[m].color }}
            />
            {METHOD_LABELS[m]}
          </li>
        ))}
      </ul>
      <EvaluationIndicatorTable
        run={run}
        visible={visible}
        onToggle={(m, checked) => setVisible((v) => ({ ...v, [m]: checked }))}
        numbersFor={numbersFor}
        onNumbersFor={setNumbersFor}
      />
    </div>
  )
}
