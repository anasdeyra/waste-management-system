package com.ase.wastems.service;

import com.ase.wastems.optimization.PlanningMethod;

/** Per-batch aggregate of one planning method over one group of runs (group ALL, SIZE or PROFILE). */
public record AggregateRow(String group, String value, PlanningMethod method, int runs, int comparableRuns,
        int excludedRuns, Double meanDistanceM, Double meanDurationS, Double meanDistanceChangePct,
        Double minDistanceChangePct, Double maxDistanceChangePct, Double meanDurationChangePct,
        Double minDurationChangePct, Double maxDurationChangePct, Double meanExecutionTimeMs) {}
