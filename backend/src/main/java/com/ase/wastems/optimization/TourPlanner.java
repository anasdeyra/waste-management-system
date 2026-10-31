package com.ase.wastems.optimization;

/** A planning method turns a scenario into an ordered tour. */
public interface TourPlanner {

    /** The method implemented by this planner. */
    PlanningMethod method();

    /**
     * Builds an ordered tour for the given scenario.
     *
     * @param scenario the scenario to plan
     * @return the resulting plan, including the measured indicators and the constraint checks
     */
    TourPlan plan(PlanningScenario scenario);
}
