package com.ase.wastems.optimization;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared behaviour of the planning methods: distance measurement, duration estimation and
 * constraint checking (FR-18, NFR-07).
 */
public abstract class AbstractTourPlanner implements TourPlanner {

    /**
     * Computes the length of a tour that starts at the depot, visits {@code order} and returns
     * to the depot.
     */
    protected double tourDistance(PlanningScenario scenario, List<ScenarioPoint> order) {
        double distance = 0;
        double currentX = scenario.depotX();
        double currentY = scenario.depotY();

        for (ScenarioPoint point : order) {
            distance += DistanceCalculator.euclidean(currentX, currentY, point.x(), point.y());
            currentX = point.x();
            currentY = point.y();
        }
        distance += DistanceCalculator.euclidean(currentX, currentY, scenario.depotX(), scenario.depotY());
        return distance;
    }

    /** Estimates the execution duration: travel time plus service time per stop (D-009, D-015). */
    protected double estimateDuration(PlanningScenario scenario, List<ScenarioPoint> order, double distanceKm) {
        double travelMinutes = distanceKm / scenario.averageSpeedKmh() * 60.0;
        return travelMinutes + (order.size() * scenario.serviceTimeMinutes());
    }

    /**
     * Counts the constraints violated by a plan.
     *
     * <p>A violation is either a missing collection point (coverage) or an excess of the
     * vehicle capacity.
     */
    protected int countViolations(PlanningScenario scenario, List<ScenarioPoint> order) {
        int violations = 0;

        if (order.size() != scenario.points().size()) {
            violations++;
        }

        double load = order.stream().mapToDouble(ScenarioPoint::demandKg).sum();
        if (load > scenario.vehicleCapacityKg()) {
            violations++;
        }

        return violations;
    }

    /** Builds the final plan of a method from the computed visit order. */
    protected TourPlan buildPlan(
            PlanningScenario scenario, List<ScenarioPoint> order, PlanningMethod method) {
        double distance = tourDistance(scenario, order);
        double duration = estimateDuration(scenario, order, distance);
        double load = order.stream().mapToDouble(ScenarioPoint::demandKg).sum();
        List<Long> visitedIds = new ArrayList<>(order.size());
        for (ScenarioPoint point : order) {
            visitedIds.add(point.id());
        }
        return new TourPlan(
                method, visitedIds, distance, duration, load, countViolations(scenario, order));
    }

    /** Returns the points of the scenario in their input order. */
    protected List<ScenarioPoint> inInputOrder(PlanningScenario scenario) {
        return new ArrayList<>(scenario.points());
    }
}
