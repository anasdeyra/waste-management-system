package com.ase.wastems.optimization;

import java.util.List;

/**
 * A planning scenario: the input of every planning method (FR-15).
 *
 * @param name identifier of the scenario, used to group evaluation results
 * @param depotX planning coordinate of the depot on the x axis, in kilometres
 * @param depotY planning coordinate of the depot on the y axis, in kilometres
 * @param points points to visit, in the order given by the caller
 * @param vehicleCapacityKg capacity of the single vehicle of the tour, in kilograms
 * @param averageSpeedKmh average travel speed, in kilometres per hour
 * @param serviceTimeMinutes service time per stop, in minutes
 */
public record PlanningScenario(
        String name,
        double depotX,
        double depotY,
        List<ScenarioPoint> points,
        double vehicleCapacityKg,
        double averageSpeedKmh,
        double serviceTimeMinutes) {

    public PlanningScenario {
        points = List.copyOf(points);
    }

    /** Total kilograms of waste requested by the scenario. */
    public double totalDemandKg() {
        return points.stream().mapToDouble(ScenarioPoint::demandKg).sum();
    }
}
