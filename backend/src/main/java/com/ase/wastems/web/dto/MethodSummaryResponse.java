package com.ase.wastems.web.dto;

import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.optimization.PlanningMethod;

/** Headline figures of one method in a run list. */
public record MethodSummaryResponse(PlanningMethod method, boolean feasible, Double distanceM, Double durationS) {

    public static MethodSummaryResponse from(MethodResult r) {
        return new MethodSummaryResponse(r.getMethod(), r.isFeasible(), r.getDistanceM(), r.getDurationS());
    }
}
