package com.ase.wastems.web.dto;

import com.ase.wastems.domain.StopDemand;
import java.math.BigDecimal;
import java.util.UUID;

/** Planned demand of one category at a tour stop. */
public record StopDemandResponse(
        UUID id, UUID categoryId, String categoryName, BigDecimal plannedQuantityKg) {

    public static StopDemandResponse from(StopDemand d) {
        return new StopDemandResponse(
                d.getId(),
                d.getCategory().getId(),
                d.getCategory().getName(),
                d.getPlannedQuantityKg());
    }
}
