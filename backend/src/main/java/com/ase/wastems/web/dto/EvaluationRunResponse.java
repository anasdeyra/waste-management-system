package com.ase.wastems.web.dto;

import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.ProposalDecision;
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
                r.getStatus(), r.getFailureCode(), r.getFailureMessage(), r.getCreatedAt(), r.getCreatedBy(),
                r.getRoadGraphVersion(), r.getRoutingEngine(), r.sourceSnapshotIdList(),
                new EvaluationDepotResponse(r.getDepotName(), r.getDepotLongitude(), r.getDepotLatitude(),
                        r.getDepotSourceIdentifier()),
                r.getVehicleCapacityKg(), r.getServiceTimeSeconds(), r.getPointCount(), r.getTotalDemandKg(),
                r.getStops().stream().map(EvaluationStopResponse::from).toList(),
                r.getResults().stream().map(m -> MethodResultResponse.from(m, r.getStops())).toList(),
                r.getDecision(), r.getDecidedBy(), r.getDecidedAt());
    }
}
