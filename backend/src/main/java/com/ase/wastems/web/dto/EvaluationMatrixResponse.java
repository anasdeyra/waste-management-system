package com.ase.wastems.web.dto;

import java.util.List;
import java.util.UUID;

/** Retained directed distance (m) and travel time (s) matrices; node 0 is the depot. */
public record EvaluationMatrixResponse(UUID runId, String roadGraphVersion, int nodeCount,
        List<String> nodeLabels, double[][] distanceM, double[][] travelTimeS) {}
