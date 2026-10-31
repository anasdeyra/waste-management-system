package com.ase.wastems.optimization;

import java.util.List;

/**
 * The result of applying a planning method to a scenario (FR-16, FR-18).
 *
 * @param method the method that produced the plan
 * @param visitedPointIds identifiers of the visited points, in visit order
 * @param totalDistanceKm total travelled distance, in kilometres
 * @param estimatedDurationMinutes estimated execution duration, in minutes
 * @param totalLoadKg total collected load, in kilograms
 * @param constraintViolations number of violated constraints; zero for a valid plan
 */
public record TourPlan(
        PlanningMethod method,
        List<Long> visitedPointIds,
        double totalDistanceKm,
        double estimatedDurationMinutes,
        double totalLoadKg,
        int constraintViolations) {

    public TourPlan {
        visitedPointIds = List.copyOf(visitedPointIds);
    }

    /** Number of collection points covered by the plan. */
    public int coveredPoints() {
        return visitedPointIds.size();
    }
}
