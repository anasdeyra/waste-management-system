package com.ase.wastems.optimization;

/**
 * The four tour planning methods compared on every scenario (D-008, D-022, D-052). The declared
 * order is the evaluation and display order.
 */
public enum PlanningMethod {
    INPUT_ORDER,
    SWEEP,
    NEAREST_NEIGHBOR,
    NN_PLUS_2_OPT
}
