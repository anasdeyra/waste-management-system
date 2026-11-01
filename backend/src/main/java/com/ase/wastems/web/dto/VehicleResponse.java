package com.ase.wastems.web.dto;

import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.Vehicle;
import java.math.BigDecimal;
import java.util.UUID;

/** A vehicle as seen by the client. */
public record VehicleResponse(
        UUID id, String registration, BigDecimal capacityKg, ResourceStatus status, DataOrigin origin) {

    public static VehicleResponse from(Vehicle v) {
        return new VehicleResponse(
                v.getId(), v.getRegistration(), v.getCapacityKg(), v.getStatus(), v.getOrigin());
    }
}
