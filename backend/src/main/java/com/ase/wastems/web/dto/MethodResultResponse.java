package com.ase.wastems.web.dto;

import com.ase.wastems.domain.EvaluationStop;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.service.EvaluationComparison;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.util.List;
import java.util.UUID;

/** One method's retained outcome; {@code legs} is the GeoJSON FeatureCollection emitted as an object. */
public record MethodResultResponse(
        PlanningMethod method,
        boolean feasible,
        String infeasibilityReason,
        List<Integer> visitOrder,
        List<UUID> collectionPointOrder,
        Double distanceM,
        Double travelTimeS,
        Double durationS,
        double executionTimeMs,
        boolean coverageOk,
        boolean capacityOk,
        String algorithmConfiguration,
        Double distanceChangePct,
        Double durationChangePct,
        Double distanceChangeVsSweepPct,
        Double durationChangeVsSweepPct,
        @JsonRawValue String legs) {

    public static MethodResultResponse from(MethodResult r, List<EvaluationStop> stops, MethodResult inputOrder,
            MethodResult sweep, boolean runCompleted) {
        List<Integer> order = r.visitOrderList();
        List<UUID> pointOrder = order.stream().map(i -> stops.get(i - 1).getCollectionPointId()).toList();
        boolean vs = EvaluationComparison.comparedWithSweep(r.getMethod()) && sweep != null;
        return new MethodResultResponse(r.getMethod(), r.isFeasible(), r.getInfeasibilityReason(), order,
                pointOrder, r.getDistanceM(), r.getTravelTimeS(), r.getDurationS(),
                r.getExecutionNanos() / 1e6, r.isCoverageOk(), r.isCapacityOk(), r.getAlgorithmConfiguration(),
                inputOrder == null ? null : EvaluationComparison.changePct(runCompleted, r.isFeasible(),
                        r.getDistanceM(), inputOrder.isFeasible(), inputOrder.getDistanceM()),
                inputOrder == null ? null : EvaluationComparison.changePct(runCompleted, r.isFeasible(),
                        r.getDurationS(), inputOrder.isFeasible(), inputOrder.getDurationS()),
                vs ? EvaluationComparison.changePct(runCompleted, r.isFeasible(), r.getDistanceM(),
                        sweep.isFeasible(), sweep.getDistanceM()) : null,
                vs ? EvaluationComparison.changePct(runCompleted, r.isFeasible(), r.getDurationS(),
                        sweep.isFeasible(), sweep.getDurationS()) : null,
                r.getLegsGeoJson());
    }
}
