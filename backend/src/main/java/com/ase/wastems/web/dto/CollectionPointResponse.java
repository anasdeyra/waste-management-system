package com.ase.wastems.web.dto;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** A collection point with its demand entries as seen by the client. */
public record CollectionPointResponse(
        UUID id,
        String name,
        double longitude,
        double latitude,
        CollectionPointStatus status,
        DataOrigin origin,
        String sourceDataset,
        String sourceIdentifier,
        String sourceOwnershipType,
        List<DemandEntryResponse> demands,
        BigDecimal totalDemandKg) {

    public static CollectionPointResponse from(CollectionPoint p) {
        List<DemandEntryResponse> demands =
                p.getDemands().stream()
                        .map(DemandEntryResponse::from)
                        .sorted(
                                Comparator.comparing(
                                        DemandEntryResponse::categoryName,
                                        String.CASE_INSENSITIVE_ORDER))
                        .toList();
        return new CollectionPointResponse(
                p.getId(),
                p.getName(),
                p.getLongitude(),
                p.getLatitude(),
                p.getStatus(),
                p.getOrigin(),
                p.getSourceDataset(),
                p.getSourceIdentifier(),
                p.getSourceOwnershipType(),
                demands,
                p.totalDemandKg());
    }
}
