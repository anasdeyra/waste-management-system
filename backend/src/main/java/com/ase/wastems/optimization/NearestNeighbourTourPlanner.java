package com.ase.wastems.optimization;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Capacity-aware nearest-neighbour construction (D-008).
 *
 * <p>The planner repeatedly moves to the closest point that still fits into the remaining
 * capacity of the vehicle. Points that would exceed the capacity are left out and reported as a
 * constraint violation, so an infeasible scenario stays visible instead of being silently
 * dropped (FR-18).
 */
@Component
public class NearestNeighbourTourPlanner extends AbstractTourPlanner {

    @Override
    public PlanningMethod method() {
        return PlanningMethod.NEAREST_NEIGHBOR;
    }

    @Override
    public TourPlan plan(PlanningScenario scenario) {
        List<ScenarioPoint> remaining = new ArrayList<>(scenario.points());
        List<ScenarioPoint> order = new ArrayList<>(remaining.size());

        double currentX = scenario.depotX();
        double currentY = scenario.depotY();
        double load = 0;

        while (!remaining.isEmpty()) {
            ScenarioPoint closest = null;
            double closestDistance = Double.MAX_VALUE;

            for (ScenarioPoint candidate : remaining) {
                double distance = DistanceCalculator.euclidean(currentX, currentY, candidate.x(), candidate.y());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = candidate;
                }
            }

            if (closest == null) {
                break;
            }

            if (load + closest.demandKg() > scenario.vehicleCapacityKg()) {
                // The point does not fit; keep it aside and try the next closest one.
                remaining.remove(closest);
                continue;
            }

            order.add(closest);
            remaining.remove(closest);
            load += closest.demandKg();
            currentX = closest.x();
            currentY = closest.y();
        }

        return buildPlan(scenario, order, method());
    }
}
