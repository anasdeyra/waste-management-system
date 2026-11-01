package com.ase.wastems.web.dto;

import com.ase.wastems.optimization.PlanningMethod;

/** One method's indicators in a run list item (no route geometry, no visit order). */
public record EvaluationMethodBriefResponse(PlanningMethod method, boolean feasible, Double distanceM,
        Double durationS, double executionTimeMs, boolean coverageOk, boolean capacityOk,
        Double distanceChangePct, Double durationChangePct) {}
