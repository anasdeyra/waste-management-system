package com.ase.wastems.web;

import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStop;
import com.ase.wastems.service.EvaluationService;
import com.ase.wastems.service.FrozenScenario;
import com.ase.wastems.service.NotFoundException;
import com.ase.wastems.service.TourService;
import com.ase.wastems.web.dto.EvaluationMatrixResponse;
import com.ase.wastems.web.dto.EvaluationRunResponse;
import com.ase.wastems.web.dto.EvaluationRunSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Three-method evaluation API (US-21, US-22); PLANNER and ADMIN only (SecurityConfig). The
 * controller orchestrates: it freezes the draft through the tour service and hands the immutable
 * scenario to the evaluation service, which never calls back into tours.
 */
@RestController
public class EvaluationController {

    private final TourService tours;
    private final EvaluationService evaluations;
    private final ObjectMapper objectMapper;

    public EvaluationController(TourService tours, EvaluationService evaluations, ObjectMapper objectMapper) {
        this.tours = tours;
        this.evaluations = evaluations;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/tours/{id}/evaluations")
    public ResponseEntity<EvaluationRunResponse> evaluate(@PathVariable UUID id, Authentication auth) {
        FrozenScenario scenario = tours.loadDraftAndBuildScenario(id);
        EvaluationRun run = evaluations.evaluate(scenario, auth.getName());
        return ResponseEntity.created(URI.create("/api/evaluations/" + run.getId()))
                .body(EvaluationRunResponse.from(run));
    }

    @GetMapping("/api/tours/{id}/evaluations")
    public List<EvaluationRunSummaryResponse> listForTour(@PathVariable UUID id) {
        tours.get(id);
        return evaluations.listForTour(id).stream().map(EvaluationRunSummaryResponse::from).toList();
    }

    @GetMapping("/api/evaluations/{runId}")
    public EvaluationRunResponse get(@PathVariable UUID runId) {
        return EvaluationRunResponse.from(evaluations.get(runId));
    }

    @GetMapping("/api/evaluations/{runId}/matrix")
    public EvaluationMatrixResponse matrix(@PathVariable UUID runId) {
        EvaluationRun run = evaluations.get(runId);
        if (run.getMatrixJson() == null) {
            throw new NotFoundException(
                    "Evaluation run " + runId + " has no retained matrix (routing failed).");
        }
        try {
            JsonNode doc = objectMapper.readTree(run.getMatrixJson());
            int n = doc.get("nodeCount").asInt();
            List<String> labels = new ArrayList<>();
            labels.add("Depot: " + run.getDepotName());
            for (EvaluationStop s : run.getStops()) {
                labels.add(s.getInputIndex() + ": " + s.getName());
            }
            return new EvaluationMatrixResponse(run.getId(), run.getRoadGraphVersion(), n, labels,
                    objectMapper.treeToValue(doc.get("distanceM"), double[][].class),
                    objectMapper.treeToValue(doc.get("travelTimeS"), double[][].class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Retained matrix of run " + runId + " is unreadable", e);
        }
    }
}
