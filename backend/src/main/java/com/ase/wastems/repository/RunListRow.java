package com.ase.wastems.repository;

import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.ScenarioKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Run list projection; never carries the matrix or the route geometry. */
public record RunListRow(UUID id, ScenarioKind scenarioKind, UUID batchId, String scenarioKey,
        String demandProfile, Long seed, Integer repetition, UUID tourId, Integer pointCount,
        EvaluationStatus status, String failureCode, Instant createdAt, String createdBy,
        String roadGraphVersion, BigDecimal totalDemandKg, BigDecimal vehicleCapacityKg) {}
