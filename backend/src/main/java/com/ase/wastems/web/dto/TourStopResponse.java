package com.ase.wastems.web.dto;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.domain.TourStopStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** A tour stop: live point data plus the planned demand snapshot. */
public record TourStopResponse(
        UUID id,
        int visitOrder,
        TourStopStatus status,
        UUID collectionPointId,
        String name,
        double longitude,
        double latitude,
        CollectionPointStatus pointStatus,
        boolean optimizationEligible,
        String sourceDataset,
        String sourceIdentifier,
        String sourceOwnershipType,
        List<StopDemandResponse> plannedDemands,
        BigDecimal plannedTotalKg,
        String incidentReason,
        Instant outcomeRecordedAt,
        String outcomeRecordedBy,
        List<CollectedQuantityResponse> actualQuantities,
        BigDecimal actualTotalKg,
        List<OutcomeCorrectionResponse> corrections) {

    public static TourStopResponse from(TourStop s) {
        CollectionPoint p = s.getCollectionPoint();
        List<StopDemandResponse> demands =
                s.getStopDemands().stream()
                        .map(StopDemandResponse::from)
                        .sorted(
                                Comparator.comparing(
                                        StopDemandResponse::categoryName,
                                        String.CASE_INSENSITIVE_ORDER))
                        .toList();
        List<CollectedQuantityResponse> actuals =
                s.getActualQuantities().stream()
                        .map(CollectedQuantityResponse::from)
                        .sorted(
                                Comparator.comparing(
                                        CollectedQuantityResponse::categoryName,
                                        String.CASE_INSENSITIVE_ORDER))
                        .toList();
        return new TourStopResponse(
                s.getId(),
                s.getVisitOrder(),
                s.getStatus(),
                p.getId(),
                p.getName(),
                p.getLongitude(),
                p.getLatitude(),
                p.getStatus(),
                p.isOptimizationEligible(),
                p.getSourceDataset(),
                p.getSourceIdentifier(),
                p.getSourceOwnershipType(),
                demands,
                s.plannedTotalKg(),
                s.getIncidentReason(),
                s.getOutcomeRecordedAt(),
                s.getOutcomeRecordedBy() == null ? null : s.getOutcomeRecordedBy().getUsername(),
                actuals,
                actuals.isEmpty() ? null : s.actualTotalKg(),
                s.getCorrections().stream().map(OutcomeCorrectionResponse::from).toList());
    }
}
