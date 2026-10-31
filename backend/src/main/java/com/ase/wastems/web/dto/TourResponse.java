package com.ase.wastems.web.dto;

import com.ase.wastems.domain.Tour;
import java.time.LocalDate;
import java.util.List;

/** Read model of a collection tour returned by the REST API. */
public record TourResponse(
        Long id,
        LocalDate plannedDate,
        String status,
        Long vehicleId,
        String vehiclePlate,
        Long driverId,
        String driverName,
        List<Long> collectorIds,
        List<String> collectorNames,
        double totalDistanceKm,
        double estimatedDurationMinutes,
        double totalLoadKg,
        List<TourStopResponse> stops) {

    public static TourResponse from(Tour tour, List<String> collectorNames, List<Long> collectorIds) {
        return new TourResponse(
                tour.getId(),
                tour.getPlannedDate(),
                tour.getStatus().name(),
                tour.getVehicle().getId(),
                tour.getVehicle().getPlate(),
                tour.getDriver().getId(),
                tour.getDriver().getFullName(),
                collectorIds,
                collectorNames,
                tour.getTotalDistanceKm(),
                tour.getEstimatedDurationMinutes(),
                tour.getTotalLoadKg(),
                tour.getStops().stream().map(TourStopResponse::from).toList());
    }
}
