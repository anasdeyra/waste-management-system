package com.ase.wastems.optimization;

/**
 * Planning method applied to a scenario (D-008).
 *
 * <p>The three methods are deterministic so that every reported number can be reproduced.
 */
public enum PlanningMethod {
    /** Collection points are visited in the order they appear in the scenario. */
    BASELINE("Baseline (input order)"),
    /** Capacity-aware nearest-neighbour construction. */
    NEAREST_NEIGHBOR("Nearest neighbour"),
    /** Nearest-neighbour construction followed by a 2-opt local search. */
    OPTIMIZED("Nearest neighbour + 2-opt");

    private final String label;

    PlanningMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
