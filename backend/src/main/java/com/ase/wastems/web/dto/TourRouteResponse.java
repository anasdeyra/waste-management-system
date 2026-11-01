package com.ase.wastems.web.dto;

import com.ase.wastems.domain.Tour;
import com.ase.wastems.optimization.PlanningMethod;
import com.fasterxml.jackson.annotation.JsonRawValue;
import java.util.UUID;

/** The road route attached to a tour; {@code legs} is the GeoJSON FeatureCollection emitted as an object. */
public record TourRouteResponse(UUID evaluationRunId, PlanningMethod method, String roadGraphVersion,
        Double distanceM, Double durationS, @JsonRawValue String legs) {

    public static TourRouteResponse from(Tour t) {
        return t.hasRoute()
                ? new TourRouteResponse(t.getRouteEvaluationRunId(), t.getRouteMethod(), t.getRoadGraphVersion(),
                        t.getRouteDistanceM(), t.getRouteDurationS(), t.getRoadLegsGeoJson())
                : null;
    }
}
