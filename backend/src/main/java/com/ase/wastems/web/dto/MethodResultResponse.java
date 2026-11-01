package com.ase.wastems.web.dto;

import com.ase.wastems.domain.EvaluationStop;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.optimization.PlanningMethod;
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
        @JsonRawValue String legs) {

    public static MethodResultResponse from(MethodResult r, List<EvaluationStop> stops) {
        List<Integer> order = r.visitOrderList();
        List<UUID> pointOrder = order.stream().map(i -> stops.get(i - 1).getCollectionPointId()).toList();
        return new MethodResultResponse(r.getMethod(), r.isFeasible(), r.getInfeasibilityReason(), order,
                pointOrder, r.getDistanceM(), r.getTravelTimeS(), r.getDurationS(),
                r.getExecutionNanos() / 1e6, r.isCoverageOk(), r.isCapacityOk(), r.getAlgorithmConfiguration(),
                r.getLegsGeoJson());
    }
}
