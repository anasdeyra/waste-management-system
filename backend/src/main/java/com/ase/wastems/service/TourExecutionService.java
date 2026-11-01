package com.ase.wastems.service;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourIssue;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.TourRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
    private final TourService tourService;

    public TourExecutionService(TourRepository tours, AppUserRepository users, AgentRepository agents,
            TourService tourService) {
        this.tours = tours;
        this.users = users;
        this.agents = agents;
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
