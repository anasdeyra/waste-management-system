package com.ase.wastems.optimization;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The documented baseline: collection points are visited in the order given by the planner
 * (D-008).
 */
@Component
public class BaselineTourPlanner extends AbstractTourPlanner {

    @Override
    public PlanningMethod method() {
        return PlanningMethod.BASELINE;
    }

    @Override
    public TourPlan plan(PlanningScenario scenario) {
        List<ScenarioPoint> order = inInputOrder(scenario);
        return buildPlan(scenario, order, method());
    }
}
