package com.ase.wastems.web.dto;

import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import java.math.BigDecimal;
import java.util.UUID;

/** A demand entry as seen by the client. */
public record DemandEntryResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        boolean categoryActive,
        BigDecimal quantityKg,
        DataOrigin origin) {

    public static DemandEntryResponse from(DemandEntry d) {
        return new DemandEntryResponse(
                d.getId(),
                d.getCategory().getId(),
                d.getCategory().getName(),
                d.getCategory().isActive(),
                d.getQuantityKg(),
                d.getOrigin());
    }
}
