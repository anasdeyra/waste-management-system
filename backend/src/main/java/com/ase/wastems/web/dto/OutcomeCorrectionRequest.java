package com.ase.wastems.web.dto;

import com.ase.wastems.domain.TourStopStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Body of {@code POST /api/tours/{id}/stops/{stopId}/corrections} (US-31). */
public record OutcomeCorrectionRequest(
        @NotNull TourStopStatus status,
        @Valid List<CollectedQuantityRequest> quantities,
        @Size(max = 500) String incidentReason,
        @NotBlank @Size(max = 500) String reason) {

    /** Same cross-field rules as the S7 outcome body (400 before any lookup). */
    public void validateOutcome() {
        StopOutcomeRequest.validateOutcome(status, quantities, incidentReason);
    }
}
