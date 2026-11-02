package com.ase.wastems.web.dto;

import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.service.AggregateRow;

/** One aggregate row of a batch (group ALL, SIZE or PROFILE; one row per method). */
public record EvaluationAggregateResponse(String group, String value, PlanningMethod method, int runs,
        int comparableRuns, int excludedRuns, Double meanDistanceM, Double meanDurationS,
        Double meanDistanceChangePct, Double minDistanceChangePct, Double maxDistanceChangePct,
        Double meanDurationChangePct, Double minDurationChangePct, Double maxDurationChangePct,
        Double meanExecutionTimeMs, int comparableVsSweepRuns, Double meanDistanceChangeVsSweepPct,
        Double minDistanceChangeVsSweepPct, Double maxDistanceChangeVsSweepPct,
        Double meanDurationChangeVsSweepPct) {

    public static EvaluationAggregateResponse from(AggregateRow a) {
        return new EvaluationAggregateResponse(a.group(), a.value(), a.method(), a.runs(), a.comparableRuns(),
                a.excludedRuns(), a.meanDistanceM(), a.meanDurationS(), a.meanDistanceChangePct(),
                a.minDistanceChangePct(), a.maxDistanceChangePct(), a.meanDurationChangePct(),
                a.minDurationChangePct(), a.maxDurationChangePct(), a.meanExecutionTimeMs(), a.comparableVsSweepRuns(),
                a.meanDistanceChangeVsSweepPct(), a.minDistanceChangeVsSweepPct(),
                a.maxDistanceChangeVsSweepPct(), a.meanDurationChangeVsSweepPct());
    }
}
