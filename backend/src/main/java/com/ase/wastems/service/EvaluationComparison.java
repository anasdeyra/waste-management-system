package com.ase.wastems.service;

/** The single implementation of the percent-change formula used by every evaluation view (D-050). */
public final class EvaluationComparison {

    private EvaluationComparison() {}

    /**
     * {@code (value - inputValue) / inputValue * 100} (negative = reduction); null unless the run is
     * completed, both results are feasible and the input-order value is positive.
     */
    public static Double changePct(boolean runCompleted, boolean methodFeasible, Double value,
            boolean inputFeasible, Double inputValue) {
        if (!runCompleted || !methodFeasible || !inputFeasible || value == null || inputValue == null
                || inputValue <= 0) {
            return null;
        }
        return (value - inputValue) / inputValue * 100.0;
    }
}
