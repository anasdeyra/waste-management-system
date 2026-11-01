package com.ase.wastems.web.dto;

import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Compact run entry for the list of a tour's evaluations. */
public record EvaluationRunSummaryResponse(UUID id, UUID tourId, Long tourRevision, EvaluationStatus status,
        String failureCode, Instant createdAt, String createdBy, String roadGraphVersion, int pointCount,
        List<MethodSummaryResponse> results) {

    public static EvaluationRunSummaryResponse from(EvaluationRun r) {
        return new EvaluationRunSummaryResponse(r.getId(), r.getTourId(), r.getTourRevision(), r.getStatus(),
                r.getFailureCode(), r.getCreatedAt(), r.getCreatedBy(), r.getRoadGraphVersion(),
                r.getPointCount(), r.getResults().stream().map(MethodSummaryResponse::from).toList());
    }
}
