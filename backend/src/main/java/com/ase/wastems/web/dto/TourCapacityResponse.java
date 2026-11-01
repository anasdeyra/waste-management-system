package com.ase.wastems.web.dto;

import com.ase.wastems.domain.Tour;
import java.math.BigDecimal;

/** Live capacity report; the vehicle fields are omitted when no vehicle is assigned. */
public record TourCapacityResponse(
        BigDecimal totalPlannedDemandKg,
        BigDecimal vehicleCapacityKg,
        BigDecimal remainingKg,
        boolean overCapacity) {

    public static TourCapacityResponse from(Tour t) {
        BigDecimal total = t.totalPlannedDemandKg();
        if (t.getVehicle() == null) {
            return new TourCapacityResponse(total, null, null, false);
        }
        BigDecimal cap = t.getVehicle().getCapacityKg();
        return new TourCapacityResponse(total, cap, cap.subtract(total), t.isOverCapacity());
    }
}
