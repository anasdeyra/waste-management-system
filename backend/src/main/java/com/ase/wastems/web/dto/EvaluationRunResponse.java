package com.ase.wastems.web.dto;

import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.domain.ProposalDecision;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.domain.ScenarioKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full retained evaluation run (inputs, status and, when completed, the three method results). */
public record EvaluationRunResponse(
        UUID id,
        ScenarioKind scenarioKind,
        UUID tourId,
        Long tourRevision,
        UUID batchId,
        String scenarioKey,
        String demandProfile,
        Long seed,
        Integer repetition,
        EvaluationStatus status,
        String failureCode,
        String failureMessage,
        Instant createdAt,
        String createdBy,
        String roadGraphVersion,
        String routingEngine,
        List<String> sourceSnapshotIds,
        EvaluationDepotResponse depot,
        BigDecimal vehicleCapacityKg,
        double serviceTimeSeconds,
        int pointCount,
        BigDecimal totalDemandKg,
        List<EvaluationStopResponse> stops,
        List<MethodResultResponse> results,
        ProposalDecision decision,
        String decidedBy,
        Instant decidedAt) {

    public static EvaluationRunResponse from(EvaluationRun r) {
        return new EvaluationRunResponse(r.getId(), r.getScenarioKind(), r.getTourId(), r.getTourRevision(),
                r.getBatchId(), r.getScenarioKey(), r.getDemandProfile(), r.getSeed(), r.getRepetition(),
                r.getStatus(), r.getFailureCode(), r.getFailureMessage(), r.getCreatedAt(), r.getCreatedBy(),
                r.getRoadGraphVersion(), r.getRoutingEngine(), r.sourceSnapshotIdList(),
                new EvaluationDepotResponse(r.getDepotName(), r.getDepotLongitude(), r.getDepotLatitude(),
                        r.getDepotSourceIdentifier()),
                r.getVehicleCapacityKg(), r.getServiceTimeSeconds(), r.getPointCount(), r.getTotalDemandKg(),
                r.getStops().stream().map(EvaluationStopResponse::from).toList(),
                r.getResults().stream()
                        .map(m -> MethodResultResponse.from(m, r.getStops(), inputOrder(r),
                                r.getStatus() == EvaluationStatus.COMPLETED))
                        .toList(),
                r.getDecision(), r.getDecidedBy(), r.getDecidedAt());
    }

    private static MethodResult inputOrder(EvaluationRun r) {
        return r.getResults().stream().filter(m -> m.getMethod() == PlanningMethod.INPUT_ORDER).findFirst()
                .orElse(null);
    }
}
