package com.ase.wastems.web.dto;

import com.ase.wastems.domain.CorrectionQuantity;
import java.math.BigDecimal;
import java.util.UUID;

/** A category quantity retained in an outcome correction. */
public record CorrectionQuantityResponse(UUID categoryId, String categoryName, BigDecimal quantityKg) {

    public static CorrectionQuantityResponse from(CorrectionQuantity q) {
        return new CorrectionQuantityResponse(q.getCategoryId(), q.getCategoryName(), q.getQuantityKg());
    }
}
