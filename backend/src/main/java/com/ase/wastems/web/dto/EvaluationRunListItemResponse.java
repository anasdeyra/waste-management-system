package com.ase.wastems.web.dto;

import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.ScenarioKind;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.repository.RunListRow;
import com.ase.wastems.repository.RunMethodRow;
import com.ase.wastems.service.EvaluationComparison;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Run entry of the explorer list: provenance plus per-method indicators and changes. */
public record EvaluationRunListItemResponse(UUID id, ScenarioKind scenarioKind, UUID batchId, String scenarioKey,
        String demandProfile, Long seed, Integer repetition, UUID tourId, int pointCount, EvaluationStatus status,
        String failureCode, Instant createdAt, String createdBy, String roadGraphVersion, BigDecimal totalDemandKg,
        BigDecimal vehicleCapacityKg, List<EvaluationMethodBriefResponse> results) {

    public static EvaluationRunListItemResponse from(RunListRow r, List<RunMethodRow> methodRows) {
        RunMethodRow io = methodRows.stream().filter(m -> m.method() == PlanningMethod.INPUT_ORDER).findFirst()
                .orElse(null);
        RunMethodRow sweep = methodRows.stream().filter(m -> m.method() == PlanningMethod.SWEEP).findFirst()
                .orElse(null);
        boolean completed = r.status() == EvaluationStatus.COMPLETED;
        List<EvaluationMethodBriefResponse> brief = methodRows.stream()
                .filter(m -> m.method() != null)
                .sorted(Comparator.comparing(RunMethodRow::method))
                .map(m -> {
                    boolean feasible = Boolean.TRUE.equals(m.feasible());
                    boolean ioFeasible = io != null && Boolean.TRUE.equals(io.feasible());
                    Double ioDist = io == null ? null : io.distanceM();
                    Double ioDur = io == null ? null : io.durationS();
                    boolean vs = EvaluationComparison.comparedWithSweep(m.method()) && sweep != null;
                    boolean swFeasible = vs && Boolean.TRUE.equals(sweep.feasible());
                    return new EvaluationMethodBriefResponse(m.method(), feasible, m.distanceM(), m.durationS(),
                            m.executionNanos() == null ? 0 : m.executionNanos() / 1e6,
                            Boolean.TRUE.equals(m.coverageOk()), Boolean.TRUE.equals(m.capacityOk()),
                            EvaluationComparison.changePct(completed, feasible, m.distanceM(), ioFeasible, ioDist),
                            EvaluationComparison.changePct(completed, feasible, m.durationS(), ioFeasible, ioDur),
                            vs ? EvaluationComparison.changePct(completed, feasible, m.distanceM(), swFeasible,
                                    sweep.distanceM()) : null,
                            vs ? EvaluationComparison.changePct(completed, feasible, m.durationS(), swFeasible,
                                    sweep.durationS()) : null);
                })
                .toList();
        return new EvaluationRunListItemResponse(r.id(), r.scenarioKind(), r.batchId(), r.scenarioKey(),
                r.demandProfile(), r.seed(), r.repetition(), r.tourId(),
                r.pointCount() == null ? 0 : r.pointCount(), r.status(), r.failureCode(), r.createdAt(),
                r.createdBy(), r.roadGraphVersion(), r.totalDemandKg(), r.vehicleCapacityKg(), brief);
    }
}
