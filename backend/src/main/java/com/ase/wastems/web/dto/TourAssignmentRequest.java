package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Replaces the vehicle, driver and collectors of a tour; every field is optional. */
public record TourAssignmentRequest(
        UUID vehicleId, UUID driverId, List<@NotNull UUID> collectorIds) {}
