package com.ase.wastems.web;

import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.service.StopOutcomeCommand;
import com.ase.wastems.service.TourExecutionService;
import com.ase.wastems.service.TourService;
import com.ase.wastems.web.dto.StopOutcomeRequest;
import com.ase.wastems.web.dto.TourResponse;
import com.ase.wastems.web.dto.TourSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Execution API of the assigned crew (AGENT only, SecurityConfig): assigned tours, start, stop
 * outcomes, completion (S7, D-027, D-031).
 */
@RestController
@RequestMapping("/api/tours")
public class TourExecutionController {

    private final TourExecutionService execution;
    private final TourService tourService;

    public TourExecutionController(TourExecutionService execution, TourService tourService) {
        this.execution = execution;
        this.tourService = tourService;
    }

    private TourResponse respond(Tour t) {
        return TourResponse.from(t, tourService.confirmationIssues(t), tourService.startIssues(t));
    }

    @GetMapping("/mine")
    public List<TourSummaryResponse> mine(
            @RequestParam(required = false) TourStatus status, Authentication auth) {
        return execution.listAssigned(auth.getName(), status).stream()
                .map(TourSummaryResponse::from)
                .toList();
    }

    @GetMapping("/mine/{id}")
    public TourResponse mineOne(@PathVariable UUID id, Authentication auth) {
        return respond(execution.getAssigned(id, auth.getName()));
    }

    @PostMapping("/{id}/start")
    public TourResponse start(@PathVariable UUID id, Authentication auth) {
        return respond(execution.start(id, auth.getName()));
    }

    @PostMapping("/{id}/stops/{stopId}/outcome")
    public TourResponse recordOutcome(
            @PathVariable UUID id,
            @PathVariable UUID stopId,
            @Valid @RequestBody StopOutcomeRequest req,
            Authentication auth) {
        req.validateOutcome();
        List<StopOutcomeCommand.QuantityInput> quantities = req.quantities() == null
                ? List.of()
                : req.quantities().stream()
                        .map(q -> new StopOutcomeCommand.QuantityInput(q.categoryId(), q.actualQuantityKg()))
                        .toList();
        StopOutcomeCommand cmd = new StopOutcomeCommand(req.status(), quantities, req.incidentReason());
        return respond(execution.recordOutcome(id, stopId, auth.getName(), cmd));
    }
}
