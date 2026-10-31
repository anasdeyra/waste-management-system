package com.ase.wastems.optimization;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Runs every planning method on the same scenario and compares their indicators (FR-19,
 * FR-20).
 *
 * <p>All methods see exactly the same input, so the comparison isolates the effect of the
 * planning method (NFR-07).
 */
@Service
public class PlanningComparisonService {

    private final List<TourPlanner> planners;

    public PlanningComparisonService(List<TourPlanner> planners) {
        this.planners = planners;
    }

    /** Runs every registered planning method on the scenario. */
    public List<TourPlan> compare(PlanningScenario scenario) {
        return planners.stream().map(planner -> planner.plan(scenario)).toList();
    }

    /** Returns the plan produced by the given method, or {@code null} when it is not registered. */
    public TourPlan planFor(PlanningScenario scenario, PlanningMethod method) {
        return planners.stream()
                .filter(planner -> planner.method() == method)
                .map(planner -> planner.plan(scenario))
                .findFirst()
                .orElse(null);
    }
}
