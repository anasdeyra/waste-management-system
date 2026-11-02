package com.ase.wastems.service;

import com.ase.wastems.optimization.PlanningMethod;

/** The single implementation of the percent-change formula used by every evaluation view (D-050, D-052). */
public final class EvaluationComparison {

    private EvaluationComparison() {}

    /**
     * {@code (value - referenceValue) / referenceValue * 100} (negative = reduction); null unless the run
     * is completed, both results are feasible and the reference value is positive. The reference is the
     * input order or, for the change against sweep, the sweep result.
     */
    public static Double changePct(boolean runCompleted, boolean methodFeasible, Double value,
            boolean referenceFeasible, Double referenceValue) {
        if (!runCompleted || !methodFeasible || !referenceFeasible || value == null || referenceValue == null
                || referenceValue <= 0) {
            return null;
        }
        return (value - referenceValue) / referenceValue * 100.0;
    }

    /** True for the methods that are compared against the sweep baseline (D-052). */
    public static boolean comparedWithSweep(PlanningMethod method) {
        return method == PlanningMethod.NEAREST_NEIGHBOR || method == PlanningMethod.NN_PLUS_2_OPT;
    }
}
