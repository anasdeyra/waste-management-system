package com.ase.wastems.web.dto;

import com.ase.wastems.domain.OutcomeCorrection;
import com.ase.wastems.domain.TourStopStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A retained correction of a stop outcome: previous and new values, actor, time and reason. */
public record OutcomeCorrectionResponse(
        UUID id,
        Instant correctedAt,
        String correctedBy,
        String reason,
        TourStopStatus previousStatus,
        String previousIncidentReason,
        String previousRecordedBy,
        Instant previousRecordedAt,
        List<CorrectionQuantityResponse> previousQuantities,
        TourStopStatus newStatus,
        String newIncidentReason,
        List<CorrectionQuantityResponse> newQuantities) {

    public static OutcomeCorrectionResponse from(OutcomeCorrection c) {
        return new OutcomeCorrectionResponse(
                c.getId(),
                c.getCorrectedAt(),
                c.getCorrectedBy(),
                c.getReason(),
                c.getPreviousStatus(),
                c.getPreviousIncidentReason(),
                c.getPreviousRecordedBy(),
                c.getPreviousRecordedAt(),
                c.getPreviousQuantities().stream().map(CorrectionQuantityResponse::from).toList(),
                c.getNewStatus(),
                c.getNewIncidentReason(),
                c.getNewQuantities().stream().map(CorrectionQuantityResponse::from).toList());
    }
}
