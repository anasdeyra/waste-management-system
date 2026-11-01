package com.ase.wastems.web;

import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.EvaluationStop;
import com.ase.wastems.domain.ScenarioKind;
import com.ase.wastems.repository.RunListRow;
import com.ase.wastems.repository.RunMethodRow;
import com.ase.wastems.service.EvaluationExplorerService;
import com.ase.wastems.service.RunFilter;
import com.ase.wastems.web.dto.EvaluationAggregateResponse;
import com.ase.wastems.web.dto.EvaluationBatchDetailResponse;
import com.ase.wastems.web.dto.EvaluationBatchResponse;
import com.ase.wastems.web.dto.EvaluationRunListItemResponse;
import com.ase.wastems.web.dto.EvaluationRunPageResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
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
import java.util.Map;
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
    private final EvaluationExplorerService explorer;

    public EvaluationController(TourService tours, EvaluationService evaluations, ObjectMapper objectMapper,
            EvaluationExplorerService explorer) {
        this.tours = tours;
        this.evaluations = evaluations;
        this.objectMapper = objectMapper;
        this.explorer = explorer;
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

    @GetMapping("/api/evaluations")
    public EvaluationRunPageResponse search(
            @RequestParam(required = false) ScenarioKind kind,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) String profile,
            @RequestParam(required = false) Integer pointCount,
            @RequestParam(required = false) Long seed,
            @RequestParam(required = false) EvaluationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<RunListRow> p = explorer.searchRuns(
                new RunFilter(kind, batchId, blankToNull(profile), pointCount, seed, status), page, size);
        Map<UUID, List<RunMethodRow>> rows = explorer.methodRows(p.getContent().stream().map(RunListRow::id).toList());
        List<EvaluationRunListItemResponse> items = p.getContent().stream()
                .map(r -> EvaluationRunListItemResponse.from(r, rows.getOrDefault(r.id(), List.of())))
                .toList();
        return new EvaluationRunPageResponse(items, p.getNumber(), p.getSize(), p.getTotalElements(),
                p.getTotalPages());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @GetMapping("/api/evaluations/batches")
    public List<EvaluationBatchResponse> batches() {
        return explorer.batches().stream().map(EvaluationBatchResponse::from).toList();
    }

    @GetMapping("/api/evaluations/batches/{id}")
    public EvaluationBatchDetailResponse batch(@PathVariable UUID id) {
        return new EvaluationBatchDetailResponse(EvaluationBatchResponse.from(explorer.batch(id)),
                explorer.aggregates(id).stream().map(EvaluationAggregateResponse::from).toList(),
                explorer.scenarioKeys(id));
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
