package com.ase.wastems.web.dto;

import com.ase.wastems.domain.Vehicle;

/** Read model of a collection vehicle returned by the REST API. */
public record VehicleResponse(
        Long id, String plate, String description, double capacityKg, String status) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getPlate(),
                vehicle.getDescription(),
                vehicle.getCapacityKg(),
                vehicle.getStatus().name());
    }
}
