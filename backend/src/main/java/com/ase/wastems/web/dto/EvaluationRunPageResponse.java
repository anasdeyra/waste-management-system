package com.ase.wastems.web.dto;

import java.util.List;

/** One page of evaluation runs, newest first. */
public record EvaluationRunPageResponse(List<EvaluationRunListItemResponse> items, int page, int size,
        long totalElements, int totalPages) {}
