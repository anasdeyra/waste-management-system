package com.ase.wastems.service;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.StopDemand;
import com.ase.wastems.domain.TourIssue;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.domain.TourStopStatus;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Execution of tours by their assigned crew (S7, D-027, D-031): the crew's own tours, start,
 * stop outcomes and completion. A caller is a crew member iff its account is linked to the
 * tour's driver or one of its collectors; drafts are invisible to the crew.
 */
@Service
public class TourExecutionService {

    static final String NOT_LINKED = "Your account is not linked to an operational agent.";
    static final String NOT_ASSIGNED = "You are not assigned to this tour.";

    private final TourRepository tours;
    private final AppUserRepository users;
    private final AgentRepository agents;
    private final WasteCategoryRepository categories;
    private final TourService tourService;

    public TourExecutionService(TourRepository tours, AppUserRepository users, AgentRepository agents,
            WasteCategoryRepository categories, TourService tourService) {
        this.tours = tours;
        this.users = users;
        this.agents = agents;
        this.categories = categories;
        this.tourService = tourService;
    }

    private static int rank(TourStatus s) {
        return switch (s) {
            case IN_PROGRESS -> 0;
            case PLANNED -> 1;
            case COMPLETED -> 2;
            case CANCELLED -> 3;
            case DRAFT -> 4;
        };
    }

    /** Tours whose crew contains the caller's linked agent, excluding drafts; empty when unlinked. */
    @Transactional(readOnly = true)
    public List<Tour> listAssigned(String username, TourStatus status) {
        Optional<Agent> agent = users.findByUsername(username).flatMap(agents::findByUserAccount);
        if (agent.isEmpty()) {
            return List.of();
        }
        return tours.findAssignedTo(agent.get().getId()).stream()
                .filter(t -> status == null || t.getStatus() == status)
                .sorted(Comparator.comparingInt((Tour t) -> rank(t.getStatus()))
                        .thenComparing(Tour::getPlanningDate)
                        .thenComparing(Tour::getCreatedAt))
                .map(TourService::initialized)
                .toList();
    }

    /** One assigned tour; 404 unknown, 403 not visible. */
    @Transactional(readOnly = true)
    public Tour getAssigned(UUID tourId, String username) {
        Tour tour = tours.findById(tourId)
                .orElseThrow(() -> new NotFoundException("Tour not found: " + tourId));
        return TourService.initialized(visibleTour(tour, username));
    }

    /** PLANNED to IN_PROGRESS by a crew member after revalidation (US-27). */
    @Transactional
    public Tour start(UUID tourId, String username) {
        Tour tour = lockedVisible(tourId, username);
        if (tour.getStatus() != TourStatus.PLANNED) {
            throw new ConflictException(
                    "Only a PLANNED tour can be started; this tour is " + tour.getStatus() + ".");
        }
        List<TourIssue> issues = tourService.startIssues(tour);
        if (!issues.isEmpty()) {
            throw new TourNotStartableException(issues);
        }
        tour.start(now(), username);
        tour.touch();
        return TourService.initialized(tour);
    }

    /** PENDING to COLLECTED / NOT_COLLECTED by a crew member on an IN_PROGRESS tour (US-28). */
    @Transactional
    public Tour recordOutcome(UUID tourId, UUID stopId, String username, StopOutcomeCommand cmd) {
        Tour tour = lockedVisible(tourId, username);
        TourStop stop = tour.getStops().stream()
                .filter(s -> stopId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Tour stop not found: " + stopId));
        if (tour.getStatus() != TourStatus.IN_PROGRESS) {
            throw new ConflictException(
                    "Outcomes can be recorded only on an IN_PROGRESS tour; this tour is " + tour.getStatus() + ".");
        }
        if (stop.getStatus() != TourStopStatus.PENDING) {
            throw new ConflictException("Stop " + stop.getVisitOrder() + " already has the outcome "
                    + stop.getStatus() + "; corrections are made by a planner.");
        }
        AppUser recorder = caller(username);
        Instant now = now();
        if (cmd.status() == TourStopStatus.COLLECTED) {
            stop.recordCollected(resolveQuantities(stop, cmd), cmd.incidentReason(), recorder, now);
        } else {
            stop.recordNotCollected(cmd.incidentReason(), recorder, now);
        }
        tour.touch();
        tours.flush();
        return TourService.initialized(tour);
    }

    private Map<WasteCategory, BigDecimal> resolveQuantities(TourStop stop, StopOutcomeCommand cmd) {
        Set<UUID> planned = new HashSet<>();
        for (StopDemand d : stop.getStopDemands()) {
            planned.add(d.getCategory().getId());
        }
        Map<WasteCategory, BigDecimal> resolved = new LinkedHashMap<>();
        Set<UUID> seen = new HashSet<>();
        List<WasteCategory> found = new ArrayList<>();
        for (StopOutcomeCommand.QuantityInput q : cmd.quantities()) {
            WasteCategory c = categories.findById(q.categoryId()).orElseThrow(
                    () -> new NotFoundException("Waste category not found: " + q.categoryId()));
            found.add(c);
        }
        for (WasteCategory c : found) {
            if (!seen.add(c.getId())) {
                throw new BusinessRuleException("Each waste category may appear only once.");
            }
        }
        for (int i = 0; i < found.size(); i++) {
            WasteCategory c = found.get(i);
            if (!c.isActive() && !planned.contains(c.getId())) {
                throw new BusinessRuleException("Waste category " + c.getName() + " is inactive.");
            }
            resolved.put(c, cmd.quantities().get(i).quantityKg());
        }
        return resolved;
    }

    /** IN_PROGRESS to COMPLETED when every stop is terminal (US-29). */
    @Transactional
    public Tour complete(UUID tourId, String username) {
        Tour tour = lockedVisible(tourId, username);
        if (tour.getStatus() != TourStatus.IN_PROGRESS) {
            throw new ConflictException(
                    "Only an IN_PROGRESS tour can be completed; this tour is " + tour.getStatus() + ".");
        }
        List<TourIssue> issues = tour.completionIssues();
        if (!issues.isEmpty()) {
            throw new TourNotCompletableException(issues);
        }
        tour.complete(now(), username);
        tour.touch();
        return TourService.initialized(tour);
    }

    private Tour lockedVisible(UUID tourId, String username) {
        Tour tour = tours.findByIdForUpdate(tourId)
                .orElseThrow(() -> new NotFoundException("Tour not found: " + tourId));
        visibleTour(tour, username);
        return TourService.initialized(tour);
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** The caller's account, which must exist. */
    AppUser caller(String username) {
        return users.findByUsername(username).orElseThrow(() -> new ForbiddenException(NOT_LINKED));
    }

    Tour visibleTour(Tour tour, String username) {
        Agent agent = agents.findByUserAccount(caller(username))
                .orElseThrow(() -> new ForbiddenException(NOT_LINKED));
        if (tour.isDraft() || !tour.isCrewMember(agent.getId())) {
            throw new ForbiddenException(NOT_ASSIGNED);
        }
        return tour;
    }
}
