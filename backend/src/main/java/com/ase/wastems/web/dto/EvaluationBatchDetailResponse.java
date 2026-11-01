package com.ase.wastems.web.dto;

import java.util.List;

/** A batch with its aggregates and scenario keys. */
public record EvaluationBatchDetailResponse(EvaluationBatchResponse batch,
        List<EvaluationAggregateResponse> aggregates, List<String> scenarioKeys) {}
