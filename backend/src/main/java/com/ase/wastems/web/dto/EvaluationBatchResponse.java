package com.ase.wastems.web.dto;

import com.ase.wastems.domain.BatchStatus;
import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.domain.ScenarioKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Provenance and counts of one experiment execution or CSV import. */
public record EvaluationBatchResponse(
        UUID id,
        ScenarioKind kind,
        String label,
        BatchStatus status,
        boolean canonical,
        String parameters,
        List<String> demandProfiles,
        List<Long> seeds,
        List<Integer> sizes,
        Integer repetitions,
        BigDecimal vehicleCapacityKg,
        List<String> sourceSnapshotIds,
        String roadGraphVersion,
        String routingEngine,
        String environment,
        String fileName,
        String fileSha256,
        Integer fileRowCount,
        int scenarioCount,
        int runCount,
        int completedRunCount,
        int failedRunCount,
        Instant createdAt,
        String createdBy,
        Instant finishedAt,
        String failureMessage) {

    public static EvaluationBatchResponse from(EvaluationBatch b) {
        return new EvaluationBatchResponse(b.getId(), b.getKind(), b.getLabel(), b.getStatus(), b.isCanonical(),
                b.getParameters(), b.demandProfileList(), b.seedList(), b.sizeList(), b.getRepetitions(),
                b.getVehicleCapacityKg(), b.sourceSnapshotIdList(), b.getRoadGraphVersion(), b.getRoutingEngine(),
                b.getEnvironment(), b.getFileName(), b.getFileSha256(), b.getFileRowCount(), b.getScenarioCount(),
                b.getRunCount(), b.getCompletedRunCount(), b.getFailedRunCount(), b.getCreatedAt(),
                b.getCreatedBy(), b.getFinishedAt(), b.getFailureMessage());
    }
}
