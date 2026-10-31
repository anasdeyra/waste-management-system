package com.ase.wastems.web.dto;

import java.util.List;

/** Comparison between the baseline and the optimized planning of one scenario. */
public record OptimizationComparisonResponse(
        String scenarioName,
        int pointCount,
        List<OptimizationRunResponse> results,
        double baselineDistanceKm,
        double optimizedDistanceKm,
        double distanceImprovementPercent,
        double baselineDurationMinutes,
        double optimizedDurationMinutes,
        double durationImprovementPercent) {}
