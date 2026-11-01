package com.ase.wastems.web.dto;

import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.optimization.PlanningMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One row of the tour list. */
public record TourSummaryResponse(
        UUID id,
        LocalDate planningDate,
        TourStatus status,
        DataOrigin origin,
        UUID depotId,
        String depotName,
        int stopCount,
        int terminalStopCount,
        BigDecimal totalPlannedDemandKg,
        UUID vehicleId,
        String vehicleRegistration,
        BigDecimal vehicleCapacityKg,
        UUID driverId,
        String driverName,
        int collectorCount,
        boolean overCapacity,
        boolean structurallyValid,
        int issueCount,
        Instant updatedAt,
        PlanningMethod routeMethod,
        Instant completedAt,
        Instant cancelledAt,
        String cancellationReason,
        int correctionCount) {

    public static TourSummaryResponse from(Tour t) {
        int issues = t.structuralIssues().size();
        return new TourSummaryResponse(
                t.getId(),
                t.getPlanningDate(),
                t.getStatus(),
                t.getOrigin(),
                t.getDepot().getId(),
                t.getDepot().getName(),
                t.getStops().size(),
                t.terminalStopCount(),
                t.totalPlannedDemandKg(),
                t.getVehicle() == null ? null : t.getVehicle().getId(),
                t.getVehicle() == null ? null : t.getVehicle().getRegistration(),
                t.getVehicle() == null ? null : t.getVehicle().getCapacityKg(),
                t.getDriver() == null ? null : t.getDriver().getId(),
                t.getDriver() == null ? null : t.getDriver().getDisplayName(),
                t.getCollectors().size(),
                t.isOverCapacity(),
                issues == 0,
                issues,
                t.getUpdatedAt(),
                t.getRouteMethod(),
                t.getCompletedAt(),
                t.getCancelledAt(),
                t.getCancellationReason(),
                t.correctionCount());
    }
}
