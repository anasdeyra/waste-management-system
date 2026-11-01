package com.ase.wastems.web;

import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.service.TourService;
import com.ase.wastems.web.dto.AddTourStopRequest;
import com.ase.wastems.web.dto.CancelTourRequest;
import com.ase.wastems.web.dto.ProposalDecisionRequest;
import com.ase.wastems.web.dto.ReorderTourStopsRequest;
import com.ase.wastems.web.dto.TourAssignmentRequest;
import com.ase.wastems.web.dto.TourRequest;
import com.ase.wastems.web.dto.TourResponse;
import com.ase.wastems.web.dto.TourSummaryResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tour draft API; PLANNER and ADMIN only (SecurityConfig). There is no tour delete: drafts are
 * abandoned through cancellation (S8). Stop removal answers 200 with the tour because the
 * remaining stops are renumbered. The review endpoints accept an evaluated proposal or keep the
 * input order (S6, US-24); {@code /confirmation} moves a revalidated draft to PLANNED (US-25).
 */
@RestController
@RequestMapping("/api/tours")
public class TourController {

    private final TourService service;

    public TourController(TourService service) {
        this.service = service;
    }

    private TourResponse respond(Tour t) {
        return TourResponse.from(t, service.confirmationIssues(t), service.startIssues(t));
    }

    @GetMapping
    public List<TourSummaryResponse> list(
            @RequestParam(required = false) TourStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate planningDate) {
        return service.list(status, planningDate).stream().map(TourSummaryResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<TourResponse> create(@Valid @RequestBody TourRequest req) {
        Tour t = service.create(req.planningDate(), req.depotId());
        return ResponseEntity.created(URI.create("/api/tours/" + t.getId()))
                .body(respond(t));
    }

    @GetMapping("/{id}")
    public TourResponse get(@PathVariable UUID id) {
        return respond(service.get(id));
    }

    @PutMapping("/{id}")
    public TourResponse update(@PathVariable UUID id, @Valid @RequestBody TourRequest req) {
        return respond(service.updateHeader(id, req.planningDate(), req.depotId()));
    }

    @PostMapping("/{id}/stops")
    public ResponseEntity<TourResponse> addStop(
            @PathVariable UUID id, @Valid @RequestBody AddTourStopRequest req) {
        TourStop stop = service.addStop(id, req.collectionPointId());
        return ResponseEntity.created(URI.create("/api/tours/" + id + "/stops/" + stop.getId()))
                .body(respond(stop.getTour()));
    }

    @DeleteMapping("/{id}/stops/{stopId}")
    public TourResponse removeStop(@PathVariable UUID id, @PathVariable UUID stopId) {
        return respond(service.removeStop(id, stopId));
    }

    @PutMapping("/{id}/assignment")
    public TourResponse assign(
            @PathVariable UUID id, @Valid @RequestBody TourAssignmentRequest req) {
        return respond(service.assign(id, req.vehicleId(), req.driverId(), req.collectorIds()));
    }

    @PutMapping("/{id}/stops/order")
    public TourResponse reorderStops(
            @PathVariable UUID id, @Valid @RequestBody ReorderTourStopsRequest req) {
        return respond(service.reorderStops(id, req.stopIds()));
    }

    @PostMapping("/{id}/confirmation")
    public TourResponse confirm(@PathVariable UUID id) {
        return respond(service.confirm(id));
    }

    @PostMapping("/{id}/cancellation")
    public TourResponse cancel(
            @PathVariable UUID id, @Valid @RequestBody CancelTourRequest req, Authentication auth) {
        return respond(service.cancel(id, req.reason(), auth.getName()));
    }

    @PostMapping("/{id}/proposal-acceptances")
    public TourResponse acceptProposal(
            @PathVariable UUID id, @Valid @RequestBody ProposalDecisionRequest req, Authentication auth) {
        return respond(service.acceptOptimizedProposal(id, req.evaluationRunId(), auth.getName()));
    }

    @PostMapping("/{id}/proposal-rejections")
    public TourResponse rejectProposal(
            @PathVariable UUID id, @Valid @RequestBody ProposalDecisionRequest req, Authentication auth) {
        return respond(service.rejectProposal(id, req.evaluationRunId(), auth.getName()));
    }
}
