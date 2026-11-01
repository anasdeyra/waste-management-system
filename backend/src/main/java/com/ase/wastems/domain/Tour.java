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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A single-vehicle collection tour (D-012, D-025). S4 persists DRAFT tours: depot, planning
 * date, ordered stops with planned demand snapshots, vehicle, driver and up to two collectors.
 * The revision counts structural changes so later evaluations can prove they used identical
 * inputs.
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
        return stop;
    }

    /** Removes the stop and renumbers the remaining stops 1..n. */
    public void removeStop(TourStop stop) {
        stops.remove(stop);
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
     * Structural validation of the draft (activity diagram "Draft inputs structurally valid?").
     * Covers OCL ResourcesRequiredAfterDraft, CapacityRespectedAfterDraft, the
     * {@code Tour::confirm()} precondition CompleteValidPlan and US-12; S6 adds the route
     * checks.
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
