package com.ase.wastems.service;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.EvaluationStop;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.domain.ProposalDecision;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.SourceSnapshot;
import com.ase.wastems.domain.TourIssue;
import com.ase.wastems.domain.TourIssueCode;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.TourStop;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules of tour drafts (S4): creation, header edits, stop selection and order, and
 * (T3) vehicle/crew assignment. Every returned tour has its lazy collections loaded so that
 * the web layer can map it after the transaction.
 */
@Service
public class TourService {

    private final TourRepository tours;
    private final DepotRepository depots;
    private final CollectionPointRepository points;
    private final VehicleRepository vehicles;
    private final AgentRepository agents;
    private final SourceSnapshotRepository sourceSnapshots;
    private final EvaluationRunRepository runs;
    private final AuditService audit;

    public TourService(
            TourRepository tours,
            DepotRepository depots,
            CollectionPointRepository points,
            VehicleRepository vehicles,
            AgentRepository agents,
            SourceSnapshotRepository sourceSnapshots,
            EvaluationRunRepository runs,
            AuditService audit) {
        this.audit = audit;
        this.tours = tours;
        this.depots = depots;
        this.points = points;
        this.vehicles = vehicles;
        this.agents = agents;
        this.sourceSnapshots = sourceSnapshots;
        this.runs = runs;
    }

    static Tour initialized(Tour t) {
        t.getCollectors().size();
        for (TourStop s : t.getStops()) {
            s.getStopDemands().size();
            s.getActualQuantities().size();
        }
        return t;
    }

    @Transactional(readOnly = true)
    public List<Tour> list(TourStatus status, LocalDate planningDate) {
        return tours.search(status, planningDate).stream().map(TourService::initialized).toList();
    }

    @Transactional(readOnly = true)
    public Tour get(UUID id) {
        return initialized(find(id));
    }

    /**
     * Freezes a ready DRAFT into the immutable scenario shared by the three methods (US-19, US-21):
     * the selected points with their names, coordinates and planned demand totals, the depot, the
     * vehicle capacity and the source snapshot identifiers. Read-only: the tour is never touched.
     */
    @Transactional(readOnly = true)
    public FrozenScenario loadDraftAndBuildScenario(UUID tourId) {
        Tour tour = initialized(find(tourId));
        if (!tour.isDraft()) {
            throw new ConflictException(
                    "Only a DRAFT tour can be evaluated; this tour is " + tour.getStatus() + ".");
        }
        List<TourIssue> issues = tour.structuralIssues();
        if (!issues.isEmpty()) {
            throw new BusinessRuleException(
                    "The tour is not ready for evaluation ("
                            + issues.stream().map(i -> i.code().name()).collect(Collectors.joining(", "))
                            + "): "
                            + issues.stream().map(TourIssue::message).collect(Collectors.joining(" ")));
        }
        TreeSet<String> datasets = new TreeSet<>();
        boolean userEntered = false;
        Depot d = tour.getDepot();
        if (d.getSourceDataset() == null) {
            userEntered = true;
        } else {
            datasets.add(d.getSourceDataset());
        }
        List<FrozenStop> stops = new ArrayList<>();
        for (TourStop stop : tour.getStops()) {
            CollectionPoint p = stop.getCollectionPoint();
            if (p.getSourceDataset() == null) {
                userEntered = true;
            } else {
                datasets.add(p.getSourceDataset());
            }
            stops.add(
                    new FrozenStop(
                            stop.getVisitOrder(),
                            p.getId(),
                            p.getSourceDataset(),
                            p.getSourceIdentifier(),
                            p.getName(),
                            p.getLongitude(),
                            p.getLatitude(),
                            stop.plannedTotalKg()));
        }
        List<String> ids = new ArrayList<>();
        for (String ds : datasets) {
            ids.add(
                    sourceSnapshots
                            .findBySourceDataset(ds)
                            .map(SourceSnapshot::getSnapshotId)
                            .orElse(ds + ":unknown"));
        }
        if (userEntered) {
            ids.add("USER_ENTERED");
        }
        BigDecimal capacity = tour.getVehicle().getCapacityKg();
        return new FrozenScenario(
                tour.getId(),
                tour.getRevision(),
                new FrozenDepot(d.getName(), d.getLongitude(), d.getLatitude(), d.getSourceIdentifier()),
                stops,
                capacity,
                ids);
    }

    @Transactional
    public Tour create(LocalDate planningDate, UUID depotId) {
        return create(planningDate, depotId, DataOrigin.USER_ENTERED);
    }

    @Transactional
    public Tour create(LocalDate planningDate, UUID depotId, DataOrigin origin) {
        requireNotPast(planningDate);
        Depot depot = findDepot(depotId);
        Tour saved = tours.save(new Tour(planningDate, depot, origin));
        audit.record(
                AuditAction.TOUR_CREATED,
                AuditEntityType.TOUR,
                saved.getId(),
                "planning date " + planningDate + ", depot " + depot.getName() + ", origin " + origin);
        return initialized(saved);
    }

    @Transactional
    public Tour updateHeader(UUID id, LocalDate planningDate, UUID depotId) {
        Tour tour = find(id);
        requireDraft(tour);
        if (!planningDate.equals(tour.getPlanningDate())) {
            requireNotPast(planningDate);
        }
        Depot depot = findDepot(depotId);
        tour.setPlanningDate(planningDate);
        tour.setDepot(depot);
        tour.touch();
        audit.record(
                AuditAction.TOUR_UPDATED,
                AuditEntityType.TOUR,
                tour.getId(),
                "planning date " + planningDate + ", depot " + depot.getName());
        return initialized(tour);
    }

    @Transactional
    public TourStop addStop(UUID tourId, UUID pointId) {
        Tour tour = find(tourId);
        requireDraft(tour);
        CollectionPoint point =
                points.findWithDemandsById(pointId)
                        .orElseThrow(
                                () -> new NotFoundException("Collection point not found: " + pointId));
        if (!point.isActive()) {
            throw new BusinessRuleException(
                    "Collection point " + point.getName() + " is inactive and cannot be added to a tour.");
        }
        if (!point.isOptimizationEligible()) {
            throw new BusinessRuleException(
                    "Collection point "
                            + point.getName()
                            + " is not eligible for planning (source type "
                            + point.getSourceOwnershipType()
                            + " is not a DSNY service obligation).");
        }
        if (tour.containsPoint(pointId)) {
            throw new ConflictException(
                    "Collection point " + point.getName() + " is already a stop of this tour.");
        }
        TourStop stop = tour.addStop(point);
        tour.touch();
        tours.flush();
        audit.record(
                AuditAction.TOUR_STOP_ADDED,
                AuditEntityType.TOUR,
                tour.getId(),
                "stop " + stop.getVisitOrder() + " " + point.getName() + " (point " + pointId + ")");
        initialized(tour);
        return stop;
    }

    @Transactional
    public Tour removeStop(UUID tourId, UUID stopId) {
        Tour tour = find(tourId);
        requireDraft(tour);
        TourStop stop =
                tour.findStop(stopId)
                        .orElseThrow(() -> new NotFoundException("Tour stop not found: " + stopId));
        CollectionPoint removed = stop.getCollectionPoint();
        tour.removeStop(stop);
        tour.touch();
        audit.record(
                AuditAction.TOUR_STOP_REMOVED,
                AuditEntityType.TOUR,
                tour.getId(),
                removed.getName() + " (point " + removed.getId() + ")");
        return initialized(tour);
    }

    @Transactional
    public Tour reorderStops(UUID tourId, List<UUID> stopIds) {
        Tour tour = find(tourId);
        requireDraft(tour);
        HashSet<UUID> requested = new HashSet<>(stopIds);
        HashSet<UUID> existing = new HashSet<>();
        tour.getStops().forEach(s -> existing.add(s.getId()));
        if (stopIds.size() != tour.getStops().size()
                || requested.size() != stopIds.size()
                || !requested.equals(existing)) {
            throw new BusinessRuleException("The new order must list every stop of the tour exactly once.");
        }
        tour.reorderStops(stopIds);
        tour.touch();
        audit.record(
                AuditAction.TOUR_STOPS_REORDERED,
                AuditEntityType.TOUR,
                tour.getId(),
                stopIds.size() + " stops reordered");
        return initialized(tour);
    }

    /** Replaces vehicle, driver and collectors together; the first failing rule wins. */
    @Transactional
    public Tour assign(UUID tourId, UUID vehicleId, UUID driverId, List<UUID> collectorIds) {
        Tour tour = find(tourId);
        requireDraft(tour);
        List<UUID> collectorIdList = collectorIds == null ? List.of() : collectorIds;
        if (collectorIdList.size() > 2) {
            throw new BusinessRuleException("A tour has at most two collectors.");
        }
        if (new HashSet<>(collectorIdList).size() != collectorIdList.size()) {
            throw new BusinessRuleException("Collectors must be distinct.");
        }
        if (driverId != null && collectorIdList.contains(driverId)) {
            throw new BusinessRuleException("The driver cannot also be a collector.");
        }
        Vehicle vehicle =
                vehicleId == null
                        ? null
                        : vehicles
                                .findById(vehicleId)
                                .orElseThrow(
                                        () -> new NotFoundException("Vehicle not found: " + vehicleId));
        Agent driver = driverId == null ? null : findAgent(driverId);
        List<Agent> collectors = collectorIdList.stream().map(this::findAgent).toList();

        if (vehicle != null
                && (tour.getVehicle() == null || !tour.getVehicle().getId().equals(vehicle.getId()))
                && !vehicle.isAvailable()) {
            throw new BusinessRuleException(
                    "Vehicle " + vehicle.getRegistration() + " is unavailable and cannot be assigned.");
        }
        Set<UUID> onTour = new HashSet<>();
        if (tour.getDriver() != null) {
            onTour.add(tour.getDriver().getId());
        }
        tour.getCollectors().forEach(c -> onTour.add(c.getId()));
        List<Agent> newCrew = new ArrayList<>(collectors);
        if (driver != null) {
            newCrew.add(0, driver);
        }
        for (Agent a : newCrew) {
            if (!onTour.contains(a.getId()) && !a.isAvailable()) {
                throw new BusinessRuleException(
                        "Agent " + a.getDisplayName() + " is unavailable and cannot be assigned.");
            }
        }
        tour.assign(vehicle, driver, collectors);
        tour.touch();
        audit.record(
                AuditAction.TOUR_ASSIGNED,
                AuditEntityType.TOUR,
                tour.getId(),
                "vehicle "
                        + (vehicle == null ? "none" : vehicle.getRegistration())
                        + ", driver "
                        + (driver == null ? "none" : driver.getDisplayName())
                        + ", collectors "
                        + (collectors.isEmpty()
                                ? "none"
                                : collectors.stream()
                                        .map(Agent::getDisplayName)
                                        .collect(Collectors.joining(", "))));
        return initialized(tour);
    }

    private Tour findForUpdate(UUID id) {
        return tours.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Tour not found: " + id));
    }

    private EvaluationRun findRunOfTour(Tour tour, UUID runId) {
        EvaluationRun run = runs.findById(runId)
                .orElseThrow(() -> new NotFoundException("Evaluation run not found: " + runId));
        if (!tour.getId().equals(run.getTourId())) {
            throw new BusinessRuleException("Evaluation run " + runId + " belongs to another tour.");
        }
        run.getStops().size();
        run.getResults().size();
        return run;
    }

    /** Rule steps 5-9 of the design summary (D-047); returns the result of the method to apply. */
    private MethodResult decidableResult(Tour tour, EvaluationRun run, PlanningMethod method) {
        if (!tour.isDraft()) {
            throw new ConflictException("Only a DRAFT tour can accept or reject a proposal; this tour is "
                    + tour.getStatus() + ".");
        }
        if (run.getStatus() == EvaluationStatus.ROUTING_FAILED) {
            throw new BusinessRuleException("Evaluation run " + run.getId() + " failed routing ("
                    + run.getFailureCode() + ") and has no proposal.");
        }
        if (run.getDecision() == ProposalDecision.ACCEPTED) {
            throw new ConflictException("The proposal of evaluation run " + run.getId() + " was already accepted.");
        }
        List<UUID> runPoints = run.getStops().stream().map(EvaluationStop::getCollectionPointId).toList();
        List<UUID> tourPoints = tour.getStops().stream().map(s -> s.getCollectionPoint().getId()).toList();
        if (run.getTourRevision() == null
                || run.getTourRevision().longValue() != tour.getRevision()
                || !runPoints.equals(tourPoints)) {
            throw new ConflictException("The draft changed since this evaluation (run revision "
                    + run.getTourRevision() + ", draft revision " + tour.getRevision()
                    + "); evaluate the tour again.");
        }
        MethodResult r = run.getResults().stream().filter(m -> m.getMethod() == method).findFirst().orElse(null);
        if (r == null || !r.isFeasible() || r.getLegsGeoJson() == null) {
            throw new BusinessRuleException("The " + method + " result of evaluation run " + run.getId()
                    + " is infeasible"
                    + (r != null && r.getInfeasibilityReason() != null ? ": " + r.getInfeasibilityReason() : "")
                    + ".");
        }
        return r;
    }

    private static void attach(Tour tour, EvaluationRun run, MethodResult r) {
        tour.attachRoute(run.getId(), r.getMethod(), run.getRoadGraphVersion(), r.getLegsGeoJson(),
                r.getDistanceM(), r.getDurationS());
    }

    /** Applies the NN + 2-opt order and road route of a fresh run to the draft (US-24). */
    @Transactional
    public Tour acceptOptimizedProposal(UUID tourId, UUID runId, String username) {
        Tour tour = findForUpdate(tourId);
        EvaluationRun run = findRunOfTour(tour, runId);
        MethodResult proposal = decidableResult(tour, run, PlanningMethod.NN_PLUS_2_OPT);
        Map<UUID, UUID> stopIdByPoint = new HashMap<>();
        tour.getStops().forEach(s -> stopIdByPoint.put(s.getCollectionPoint().getId(), s.getId()));
        List<UUID> newOrder = proposal.visitOrderList().stream()
                .map(i -> stopIdByPoint.get(run.getStops().get(i - 1).getCollectionPointId()))
                .toList();
        tour.reorderStops(newOrder);
        attach(tour, run, proposal);
        tour.touch();
        run.recordDecision(ProposalDecision.ACCEPTED, username, Instant.now().truncatedTo(ChronoUnit.MILLIS));
        audit.record(
                AuditAction.PROPOSAL_ACCEPTED,
                AuditEntityType.TOUR,
                tour.getId(),
                "run "
                        + run.getId()
                        + ", NN_PLUS_2_OPT "
                        + String.format(Locale.ROOT, "%.1f", proposal.getDistanceM())
                        + " m");
        return initialized(tour);
    }

    /** Keeps the current order and attaches the run's input-order road route (US-24 reject). */
    @Transactional
    public Tour rejectProposal(UUID tourId, UUID runId, String username) {
        Tour tour = findForUpdate(tourId);
        EvaluationRun run = findRunOfTour(tour, runId);
        MethodResult inputOrder = decidableResult(tour, run, PlanningMethod.INPUT_ORDER);
        attach(tour, run, inputOrder);
        tour.touch();
        run.recordDecision(ProposalDecision.REJECTED, username, Instant.now().truncatedTo(ChronoUnit.MILLIS));
        audit.record(
                AuditAction.PROPOSAL_REJECTED,
                AuditEntityType.TOUR,
                tour.getId(),
                "run "
                        + run.getId()
                        + ", input order kept, "
                        + String.format(Locale.ROOT, "%.1f", inputOrder.getDistanceM())
                        + " m");
        return initialized(tour);
    }

    /**
     * Every confirmation issue of a DRAFT: the domain issues (structural, date, missing route)
     * plus ROUTE_OUTDATED when the route's run is gone or a stop or the depot has moved since that
     * run froze its coordinates. Empty for a non-draft.
     */
    @Transactional(readOnly = true)
    public List<TourIssue> confirmationIssues(Tour tour) {
        List<TourIssue> issues = new ArrayList<>(tour.confirmationIssues(LocalDate.now()));
        if (tour.isDraft() && tour.hasRoute()) {
            routeOutdated(tour).ifPresent(issues::add);
        }
        return issues;
    }

    /** Start issues of a PLANNED tour: the domain issues plus ROUTE_OUTDATED (S6 semantics). */
    @Transactional(readOnly = true)
    public List<TourIssue> startIssues(Tour tour) {
        List<TourIssue> issues = new ArrayList<>(tour.startIssues());
        if (tour.getStatus() == TourStatus.PLANNED && tour.hasRoute()) {
            routeOutdated(tour).ifPresent(issues::add);
        }
        return issues;
    }

    private Optional<TourIssue> routeOutdated(Tour tour) {
        Optional<EvaluationRun> found = runs.findById(tour.getRouteEvaluationRunId());
        if (found.isEmpty()) {
            return Optional.of(new TourIssue(TourIssueCode.ROUTE_OUTDATED,
                    "The evaluation run of the road route is no longer available; evaluate the tour again."));
        }
        EvaluationRun run = found.get();
        if (moved(run.getDepotLongitude(), run.getDepotLatitude(), tour.getDepot().getLongitude(),
                tour.getDepot().getLatitude())) {
            return Optional.of(new TourIssue(TourIssueCode.ROUTE_OUTDATED,
                    "The depot has moved since the road route was computed; evaluate the tour again."));
        }
        Map<UUID, EvaluationStop> frozen = new HashMap<>();
        run.getStops().forEach(s -> frozen.put(s.getCollectionPointId(), s));
        for (TourStop s : tour.getStops()) {
            CollectionPoint p = s.getCollectionPoint();
            EvaluationStop f = frozen.get(p.getId());
            if (f == null || moved(f.getLongitude(), f.getLatitude(), p.getLongitude(), p.getLatitude())) {
                return Optional.of(new TourIssue(TourIssueCode.ROUTE_OUTDATED,
                        "Stop " + s.getVisitOrder() + " (" + p.getName()
                                + ") has moved since the road route was computed; evaluate the tour again."));
            }
        }
        return Optional.empty();
    }

    private static boolean moved(double lon1, double lat1, double lon2, double lat2) {
        return Math.abs(lon1 - lon2) > 1e-9 || Math.abs(lat1 - lat2) > 1e-9;
    }

    /** DRAFT -> PLANNED after full revalidation (US-25, D-025); 422 lists every blocking issue. */
    @Transactional
    public Tour confirm(UUID tourId) {
        Tour tour = findForUpdate(tourId);
        initialized(tour);
        if (!tour.isDraft()) {
            throw new ConflictException("Only a DRAFT tour can be confirmed; this tour is " + tour.getStatus() + ".");
        }
        List<TourIssue> issues = confirmationIssues(tour);
        if (!issues.isEmpty()) {
            throw new TourNotConfirmableException(issues);
        }
        tour.confirm();
        tour.touch();
        audit.record(
                AuditAction.TOUR_CONFIRMED,
                AuditEntityType.TOUR,
                tour.getId(),
                "PLANNED with " + tour.getStops().size() + " stops");
        return initialized(tour);
    }

    private Agent findAgent(UUID id) {
        return agents.findById(id).orElseThrow(() -> new NotFoundException("Agent not found: " + id));
    }

    private Tour find(UUID id) {
        return tours.findById(id).orElseThrow(() -> new NotFoundException("Tour not found: " + id));
    }

    private Depot findDepot(UUID depotId) {
        return depots.findById(depotId)
                .orElseThrow(() -> new NotFoundException("Depot not found: " + depotId));
    }

    private static void requireNotPast(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("The planning date must be today or later.");
        }
    }

    private static void requireDraft(Tour t) {
        if (!t.isDraft()) {
            throw new ConflictException(
                    "Only a DRAFT tour can be edited; this tour is " + t.getStatus() + ".");
        }
    }
}
