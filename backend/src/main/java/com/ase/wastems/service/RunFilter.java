package com.ase.wastems.service;

import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.ScenarioKind;
import java.util.UUID;

/** Optional filters of the evaluation run list. */
public record RunFilter(ScenarioKind kind, UUID batchId, String profile, Integer pointCount, Long seed,
        EvaluationStatus status) {}
