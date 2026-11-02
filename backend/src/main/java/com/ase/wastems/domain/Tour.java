package com.ase.wastems.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.ase.wastems.optimization.PlanningMethod;

/**
 * A single-vehicle collection tour (D-012, D-025). S4 persists DRAFT tours: depot, planning
 * date, ordered stops with planned demand snapshots, vehicle, driver and up to two collectors.
 * The revision counts structural changes so later evaluations can prove they used identical
 * inputs. S6 adds the road-route reference (accepted or kept proposal) and the DRAFT to PLANNED
 * confirmation. S7 adds the execution (start, stop outcomes, completion). S8 adds the
 * cancellation and the retained outcome corrections.
 */
@Entity
@Table(name = "tour")
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "planning_date", nullable = false)
    private LocalDate planningDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TourStatus status = TourStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataOrigin origin;

    @ManyToOne(optional = false)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private Agent driver;

    @ManyToMany
    @JoinTable(
            name = "tour_collector",
            joinColumns = @JoinColumn(name = "tour_id"),
            inverseJoinColumns = @JoinColumn(name = "agent_id"))
    @OrderColumn(name = "collector_position")
    private List<Agent> collectors = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("visitOrder ASC")
    private List<TourStop> stops = new ArrayList<>();

    @Column(nullable = false)
    private long revision = 0;

    @Lob
    @Column(name = "road_legs_geojson")
    private String roadLegsGeoJson;

    @Column(name = "road_graph_version", length = 80)
    private String roadGraphVersion;

    @Column(name = "route_evaluation_run_id")
    private UUID routeEvaluationRunId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "route_method", length = 20)
    private PlanningMethod routeMethod;

    @Column(name = "route_distance_m")
    private Double routeDistanceM;

    @Column(name = "route_duration_s")
    private Double routeDurationS;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "started_by", length = 50)
    private String startedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completed_by", length = 50)
    private String completedBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 50)
    private String cancelledBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_from_status", length = 20)
    private TourStatus cancelledFromStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Tour() {}

    public Tour(LocalDate planningDate, Depot depot, DataOrigin origin) {
        this.planningDate = planningDate;
        this.depot = depot;
        this.origin = origin;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getPlanningDate() {
        return planningDate;
    }

    public void setPlanningDate(LocalDate planningDate) {
        this.planningDate = planningDate;
    }

    public TourStatus getStatus() {
        return status;
    }

    public DataOrigin getOrigin() {
        return origin;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        boolean same = this.depot == depot
                || (this.depot != null && this.depot.getId() != null && this.depot.getId().equals(depot.getId()));
        if (!same) {
            clearRoute();
        }
        this.depot = depot;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Agent getDriver() {
        return driver;
    }

    public List<Agent> getCollectors() {
        return Collections.unmodifiableList(collectors);
    }

    public List<TourStop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public long getRevision() {
        return revision;
    }

    public String getRoadLegsGeoJson() {
        return roadLegsGeoJson;
    }

    public String getRoadGraphVersion() {
        return roadGraphVersion;
    }

    public UUID getRouteEvaluationRunId() {
        return routeEvaluationRunId;
    }

    public PlanningMethod getRouteMethod() {
        return routeMethod;
    }

    public Double getRouteDistanceM() {
        return routeDistanceM;
    }

    public Double getRouteDurationS() {
        return routeDurationS;
    }

    public boolean hasRoute() {
        return roadLegsGeoJson != null;
    }

    /** Attaches the road route of one method of a retained evaluation run (US-24). */
    public void attachRoute(UUID evaluationRunId, PlanningMethod method, String roadGraphVersion,
            String legsGeoJson, double distanceM, double durationS) {
        if (evaluationRunId == null || method == null || roadGraphVersion == null || legsGeoJson == null) {
            throw new IllegalArgumentException("A road route needs run, method, graph version and legs.");
        }
        this.routeEvaluationRunId = evaluationRunId;
        this.routeMethod = method;
        this.roadGraphVersion = roadGraphVersion;
        this.roadLegsGeoJson = legsGeoJson;
        this.routeDistanceM = distanceM;
        this.routeDurationS = durationS;
    }

    /** Drops the route; called by every change of stops, order or depot. */
    public void clearRoute() {
        roadLegsGeoJson = null;
        roadGraphVersion = null;
        routeEvaluationRunId = null;
        routeMethod = null;
        routeDistanceM = null;
        routeDurationS = null;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public TourStatus getCancelledFromStatus() {
        return cancelledFromStatus;
    }

    /** DRAFT, PLANNED or IN_PROGRESS (OCL Tour::cancel NotAlreadyTerminal). */
    public boolean isCancellable() {
        return status == TourStatus.DRAFT
                || status == TourStatus.PLANNED
                || status == TourStatus.IN_PROGRESS;
    }

    /** IN_PROGRESS, COMPLETED or CANCELLED: recorded outcomes may be corrected (US-31). */
    public boolean isCorrectable() {
        return status == TourStatus.IN_PROGRESS
                || status == TourStatus.COMPLETED
                || status == TourStatus.CANCELLED;
    }

    /** Not DRAFT, and not a cancelled draft: the crew sees only confirmed work (D-049). */
    public boolean isVisibleToCrew() {
        return status != TourStatus.DRAFT
                && !(status == TourStatus.CANCELLED && cancelledFromStatus == TourStatus.DRAFT);
    }

    /**
     * DRAFT/PLANNED/IN_PROGRESS -> CANCELLED (D-025, US-30). Keeps stops, outcomes, crew and route;
     * pending stops stay PENDING.
     */
    public void cancel(String reason, Instant at, String by) {
        if (!isCancellable()) {
            throw new IllegalStateException(
                    "Only a DRAFT, PLANNED or IN_PROGRESS tour can be cancelled.");
        }
        if (reason == null || reason.isBlank() || at == null || by == null) {
            throw new IllegalArgumentException("A cancellation needs a reason, a time and an actor.");
        }
        cancelledFromStatus = status;
        status = TourStatus.CANCELLED;
        cancellationReason = reason.trim();
        cancelledAt = at;
        cancelledBy = by;
    }

    /** Number of corrections over all stops. */
    public int correctionCount() {
        return stops.stream().mapToInt(s -> s.getCorrections().size()).sum();
    }

    /** True iff the agent id is the driver's or one collector's. Null-safe. */
    public boolean isCrewMember(UUID agentId) {
        if (agentId == null) {
            return false;
        }
        return (driver != null && agentId.equals(driver.getId()))
                || collectors.stream().anyMatch(c -> agentId.equals(c.getId()));
    }

    /** Number of stops with a terminal outcome. */
    public int terminalStopCount() {
        return (int) stops.stream().filter(TourStop::isTerminal).count();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDraft() {
        return status == TourStatus.DRAFT;
    }

    public boolean containsPoint(UUID pointId) {
        return stops.stream().anyMatch(s -> s.getCollectionPoint().getId().equals(pointId));
    }

    public Optional<TourStop> findStop(UUID stopId) {
        return stops.stream().filter(s -> stopId.equals(s.getId())).findFirst();
    }

    /** Appends a stop for the point; the service checks the eligibility rules. */
    public TourStop addStop(CollectionPoint point) {
        TourStop stop = new TourStop(this, point, stops.size() + 1);
        stops.add(stop);
        clearRoute();
        return stop;
    }

    /** Removes the stop and renumbers the remaining stops 1..n. */
    public void removeStop(TourStop stop) {
        stops.remove(stop);
        clearRoute();
        for (int i = 0; i < stops.size(); i++) {
            stops.get(i).setVisitOrder(i + 1);
        }
    }

    /** Applies a new order; the service guarantees an exact permutation of the stop ids. */
    public void reorderStops(List<UUID> stopIdsInNewOrder) {
        for (TourStop s : stops) {
            s.setVisitOrder(stopIdsInNewOrder.indexOf(s.getId()) + 1);
        }
        stops.sort(Comparator.comparingInt(TourStop::getVisitOrder));
        clearRoute();
    }

    public void assign(Vehicle vehicle, Agent driver, List<Agent> newCollectors) {
        this.vehicle = vehicle;
        this.driver = driver;
        this.collectors.clear();
        this.collectors.addAll(newCollectors);
    }

    public BigDecimal totalPlannedDemandKg() {
        return stops.stream().map(TourStop::plannedTotalKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isOverCapacity() {
        return vehicle != null && totalPlannedDemandKg().compareTo(vehicle.getCapacityKg()) > 0;
    }

    /** Counts a structural change (revision + 1) and stamps the update time. */
    public void touch() {
        revision++;
        updatedAt = Instant.now();
    }

    /**
     * Confirmation findings (activity diagram "All confirmation validations pass?", OCL
     * Tour::confirm()): the structural issues, then PLANNING_DATE_PAST, then ROUTE_MISSING.
     * ROUTE_OUTDATED needs the retained run and is added by TourService. Empty when not DRAFT.
     */
    public List<TourIssue> confirmationIssues(LocalDate today) {
        if (!isDraft()) {
            return List.of();
        }
        List<TourIssue> issues = new ArrayList<>(structuralIssues());
        if (planningDate.isBefore(today)) {
            issues.add(new TourIssue(TourIssueCode.PLANNING_DATE_PAST,
                    "The planning date " + planningDate + " is in the past."));
        }
        if (!hasRoute()) {
            issues.add(new TourIssue(TourIssueCode.ROUTE_MISSING,
                    "No road route is attached. Evaluate the tour, then accept the proposal or keep the input order."));
        }
        return issues;
    }

    /** DRAFT -> PLANNED (D-025). The service has checked every confirmation issue. */
    public void confirm() {
        if (!isDraft()) {
            throw new IllegalStateException("Only a DRAFT tour can be confirmed.");
        }
        if (!hasRoute()) {
            throw new IllegalStateException("A planned tour needs a versioned road route.");
        }
        status = TourStatus.PLANNED;
    }

    /**
     * Start findings (OCL Tour::start(), activity "resources remain valid?"): empty unless
     * PLANNED; otherwise the structural issues, then ROUTE_MISSING when no route is attached.
     * ROUTE_OUTDATED is added by TourService (it needs the retained run). No planning-date check.
     */
    public List<TourIssue> startIssues() {
        if (status != TourStatus.PLANNED) {
            return List.of();
        }
        List<TourIssue> issues = new ArrayList<>(structuralIssues());
        if (!hasRoute()) {
            issues.add(new TourIssue(TourIssueCode.ROUTE_MISSING,
                    "No road route is attached. A planner must evaluate the tour again."));
        }
        return issues;
    }

    /** PLANNED to IN_PROGRESS. The service has checked every start issue. */
    public void start(Instant at, String by) {
        if (status != TourStatus.PLANNED) {
            throw new IllegalStateException("Only a PLANNED tour can be started.");
        }
        if (!hasRoute()) {
            throw new IllegalStateException("A started tour needs a versioned road route.");
        }
        status = TourStatus.IN_PROGRESS;
        startedAt = at;
        startedBy = by;
    }

    /** Completion findings: empty unless IN_PROGRESS; one STOP_PENDING per pending stop. */
    public List<TourIssue> completionIssues() {
        if (status != TourStatus.IN_PROGRESS) {
            return List.of();
        }
        List<TourIssue> issues = new ArrayList<>();
        for (TourStop s : stops) {
            if (!s.isTerminal()) {
                issues.add(new TourIssue(TourIssueCode.STOP_PENDING,
                        "Stop " + s.getVisitOrder() + " (" + s.getCollectionPoint().getName()
                                + ") has no outcome yet."));
            }
        }
        return issues;
    }

    /** IN_PROGRESS to COMPLETED (D-031: every stop has an outcome). */
    public void complete(Instant at, String by) {
        if (status != TourStatus.IN_PROGRESS) {
            throw new IllegalStateException("Only an IN_PROGRESS tour can be completed.");
        }
        if (stops.isEmpty() || !completionIssues().isEmpty()) {
            throw new IllegalStateException("Every stop needs an outcome before completion.");
        }
        status = TourStatus.COMPLETED;
        completedAt = at;
        completedBy = by;
    }

    /**
     * Structural validation of the draft (activity diagram "Draft inputs structurally valid?").
     * Covers OCL ResourcesRequiredAfterDraft, CapacityRespectedAfterDraft, the
     * {@code Tour::confirm()} precondition CompleteValidPlan and US-12; confirmation adds the
     * date and route checks ({@code confirmationIssues}).
     */
    public List<TourIssue> structuralIssues() {
        List<TourIssue> issues = new ArrayList<>();
        if (stops.isEmpty()) {
            issues.add(new TourIssue(TourIssueCode.NO_STOPS, "The tour has no stops."));
        }
        if (vehicle == null) {
            issues.add(new TourIssue(TourIssueCode.NO_VEHICLE, "No vehicle is assigned."));
        }
        if (driver == null) {
            issues.add(new TourIssue(TourIssueCode.NO_DRIVER, "No driver is assigned."));
        }
        if (vehicle != null && !vehicle.isAvailable()) {
            issues.add(
                    new TourIssue(
                            TourIssueCode.VEHICLE_UNAVAILABLE,
                            "Vehicle " + vehicle.getRegistration() + " is unavailable."));
        }
        if (driver != null && !driver.isAvailable()) {
            issues.add(
                    new TourIssue(
                            TourIssueCode.DRIVER_UNAVAILABLE,
                            "Driver " + driver.getDisplayName() + " is unavailable."));
        }
        for (Agent c : collectors) {
            if (!c.isAvailable()) {
                issues.add(
                        new TourIssue(
                                TourIssueCode.COLLECTOR_UNAVAILABLE,
                                "Collector " + c.getDisplayName() + " is unavailable."));
            }
        }
        for (TourStop s : stops) {
            CollectionPoint p = s.getCollectionPoint();
            if (!p.isActive()) {
                issues.add(
                        new TourIssue(
                                TourIssueCode.INACTIVE_STOP,
                                "Stop "
                                        + s.getVisitOrder()
                                        + " ("
                                        + p.getName()
                                        + ") is an inactive collection point."));
            } else if (!p.isOptimizationEligible()) {
                issues.add(
                        new TourIssue(
                                TourIssueCode.INELIGIBLE_STOP,
                                "Stop "
                                        + s.getVisitOrder()
                                        + " ("
                                        + p.getName()
                                        + ") is not eligible for planning (source type "
                                        + p.getSourceOwnershipType()
                                        + ")."));
            }
        }
        if (isOverCapacity()) {
            BigDecimal total = totalPlannedDemandKg();
            BigDecimal cap = vehicle.getCapacityKg();
            issues.add(
                    new TourIssue(
                            TourIssueCode.OVER_CAPACITY,
                            "Planned demand "
                                    + total.toPlainString()
                                    + " kg exceeds vehicle capacity "
                                    + cap.toPlainString()
                                    + " kg by "
                                    + total.subtract(cap).toPlainString()
                                    + " kg."));
        }
        return issues;
    }
}
