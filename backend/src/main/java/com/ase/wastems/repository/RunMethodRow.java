package com.ase.wastems.repository;

import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.optimization.PlanningMethod;

/** One method result of a run (null method for a routing-failed run), without LOB columns. */
public record RunMethodRow(java.util.UUID runId, EvaluationStatus status, String scenarioKey,
        String demandProfile, Integer pointCount, PlanningMethod method, Boolean feasible,
        Double distanceM, Double durationS, Long executionNanos, Boolean coverageOk,
        Boolean capacityOk) {}
