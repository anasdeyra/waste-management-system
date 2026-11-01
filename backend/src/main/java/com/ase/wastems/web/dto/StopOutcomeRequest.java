package com.ase.wastems.web.dto;

import com.ase.wastems.domain.TourStopStatus;
import com.ase.wastems.service.InvalidRequestException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Outcome of one stop (D-031): collected quantities by category or a not-collected reason. */
public record StopOutcomeRequest(
        @NotNull TourStopStatus status,
        @Valid List<CollectedQuantityRequest> quantities,
        @Size(max = 500) String incidentReason) {

    /** Cross-field rules of D-031 (400 before any lookup). */
    public void validateOutcome() {
        validateOutcome(status, quantities, incidentReason);
    }

    /** Shared with {@link OutcomeCorrectionRequest}. */
    public static void validateOutcome(
            TourStopStatus status, List<CollectedQuantityRequest> quantities, String incidentReason) {
        if (status == TourStopStatus.PENDING) {
            throw new InvalidRequestException("status", "must be COLLECTED or NOT_COLLECTED");
        }
        boolean noQuantities = quantities == null || quantities.isEmpty();
        if (status == TourStopStatus.COLLECTED && noQuantities) {
            throw new InvalidRequestException("quantities", "a collected stop needs at least one quantity");
        }
        if (status == TourStopStatus.NOT_COLLECTED) {
            if (incidentReason == null || incidentReason.isBlank()) {
                throw new InvalidRequestException("incidentReason", "a not-collected stop needs a reason");
            }
            if (!noQuantities) {
                throw new InvalidRequestException("quantities", "a not-collected stop has no quantities");
            }
        }
    }
}
