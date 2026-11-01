package com.ase.wastems.web.dto;

import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourIssue;
import com.ase.wastems.domain.TourStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A tour draft with stops, crew, capacity and validation findings. */
public record TourResponse(
        UUID id,
        LocalDate planningDate,
        TourStatus status,
        DataOrigin origin,
        long revision,
        Instant createdAt,
        Instant updatedAt,
        DepotResponse depot,
        VehicleResponse vehicle,
        AgentResponse driver,
        List<AgentResponse> collectors,
        List<TourStopResponse> stops,
        TourCapacityResponse capacity,
        boolean structurallyValid,
        List<TourIssueResponse> validationIssues,
        TourRouteResponse route,
        List<TourIssueResponse> confirmationIssues,
        boolean readyToConfirm,
        Instant startedAt,
        String startedBy,
        Instant completedAt,
        String completedBy,
        List<TourIssueResponse> startIssues,
        boolean readyToStart,
        boolean readyToComplete) {

    public static TourResponse from(
            Tour t, List<TourIssue> confirmationIssues, List<TourIssue> startIssues) {
        List<TourIssue> issues = t.structuralIssues();
        return new TourResponse(
                t.getId(),
                t.getPlanningDate(),
                t.getStatus(),
                t.getOrigin(),
                t.getRevision(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                DepotResponse.from(t.getDepot()),
                t.getVehicle() == null ? null : VehicleResponse.from(t.getVehicle()),
                t.getDriver() == null ? null : AgentResponse.from(t.getDriver()),
                t.getCollectors().stream().map(AgentResponse::from).toList(),
                t.getStops().stream().map(TourStopResponse::from).toList(),
                TourCapacityResponse.from(t),
                issues.isEmpty(),
                issues.stream().map(TourIssueResponse::from).toList(),
                TourRouteResponse.from(t),
                confirmationIssues.stream().map(TourIssueResponse::from).toList(),
                t.isDraft() && confirmationIssues.isEmpty(),
                t.getStartedAt(),
                t.getStartedBy(),
                t.getCompletedAt(),
                t.getCompletedBy(),
                startIssues.stream().map(TourIssueResponse::from).toList(),
                t.getStatus() == TourStatus.PLANNED && startIssues.isEmpty(),
                t.getStatus() == TourStatus.IN_PROGRESS
                        && !t.getStops().isEmpty()
                        && t.completionIssues().isEmpty());
    }
}
