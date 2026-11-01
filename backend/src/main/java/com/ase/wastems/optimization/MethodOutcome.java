package com.ase.wastems.optimization;

import java.util.List;

/**
 * Result of one planning method on one scenario: the visit order (stop nodes only, depot
 * implicit at both ends), the indicators recomputed from the directed matrix, the measured
 * execution time and the identical validation checks applied to every method. Infeasible
 * outcomes carry an empty order, no indicators and a reason.
 */
public record MethodOutcome(
        PlanningMethod method,
        boolean feasible,
        List<Integer> visitOrder,
        Double distanceM,
        Double travelTimeS,
        Double durationS,
        long executionNanos,
        boolean coverageOk,
        boolean capacityOk,
        String infeasibilityReason,
        String algorithmConfiguration) {}
