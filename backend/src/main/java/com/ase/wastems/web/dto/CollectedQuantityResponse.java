package com.ase.wastems.web.dto;

import com.ase.wastems.domain.CollectedQuantity;
import java.math.BigDecimal;
import java.util.UUID;

/** Actual quantity of one waste category collected at a stop. */
public record CollectedQuantityResponse(
        UUID id, UUID categoryId, String categoryName, BigDecimal actualQuantityKg) {

    public static CollectedQuantityResponse from(CollectedQuantity q) {
        return new CollectedQuantityResponse(
                q.getId(), q.getCategory().getId(), q.getCategory().getName(), q.getActualQuantityKg());
    }
}
