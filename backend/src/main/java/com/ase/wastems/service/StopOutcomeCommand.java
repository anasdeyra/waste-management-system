package com.ase.wastems.service;

import com.ase.wastems.domain.TourStopStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Outcome of a stop as the service sees it (independent of the web DTOs). */
public record StopOutcomeCommand(
        TourStopStatus status, List<QuantityInput> quantities, String incidentReason) {

    /** Actual quantity of one category. */
    public record QuantityInput(UUID categoryId, BigDecimal quantityKg) {}
}
