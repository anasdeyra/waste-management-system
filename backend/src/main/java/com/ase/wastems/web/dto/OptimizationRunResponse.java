package com.ase.wastems.web.dto;

import java.time.Instant;

/** A single recorded execution of a planning method on a scenario. */
public record OptimizationRunResponse(
        Long id,
        Instant createdAt,
        String scenarioName,
        int pointCount,
        String method,
        double totalDistanceKm,
        double estimatedDurationMinutes,
        long executionTimeMillis,
        int constraintViolations) {}
