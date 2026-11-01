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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A collection point selected in a tour, with its explicit input/visit order and its planned
 * demand snapshot. S7 adds the outcome (D-031): COLLECTED with the actual quantity per waste
 * category, or NOT_COLLECTED with a reason, plus the recording actor and time.
 */
@Entity
@Table(
        name = "tour_stop",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_tour_stop_point",
                        columnNames = {"tour_id", "collection_point_id"}))
public class TourStop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @ManyToOne(optional = false)
    @JoinColumn(name = "collection_point_id")
    private CollectionPoint collectionPoint;

    @Column(name = "visit_order", nullable = false)
    private int visitOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TourStopStatus status = TourStopStatus.PENDING;

    @OneToMany(mappedBy = "tourStop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StopDemand> stopDemands = new ArrayList<>();

    @Column(name = "incident_reason", length = 500)
    private String incidentReason;

    @Column(name = "outcome_recorded_at")
    private Instant outcomeRecordedAt;

    @ManyToOne
    @JoinColumn(name = "outcome_recorded_by_id")
    private AppUser outcomeRecordedBy;

    @OneToMany(mappedBy = "tourStop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollectedQuantity> actualQuantities = new ArrayList<>();

    protected TourStop() {}

    TourStop(Tour tour, CollectionPoint point, int visitOrder) {
        this.tour = tour;
        this.collectionPoint = point;
        this.visitOrder = visitOrder;
        for (DemandEntry d : point.getDemands()) {
            stopDemands.add(new StopDemand(this, d.getCategory(), d.getQuantityKg()));
        }
    }

    public UUID getId() {
        return id;
    }

    public Tour getTour() {
        return tour;
    }

    public CollectionPoint getCollectionPoint() {
        return collectionPoint;
    }

    public int getVisitOrder() {
        return visitOrder;
    }

    void setVisitOrder(int visitOrder) {
        this.visitOrder = visitOrder;
    }

    public TourStopStatus getStatus() {
        return status;
    }

    public List<StopDemand> getStopDemands() {
        return Collections.unmodifiableList(stopDemands);
    }

    public String getIncidentReason() {
        return incidentReason;
    }

    public Instant getOutcomeRecordedAt() {
        return outcomeRecordedAt;
    }

    public AppUser getOutcomeRecordedBy() {
        return outcomeRecordedBy;
    }

    public List<CollectedQuantity> getActualQuantities() {
        return Collections.unmodifiableList(actualQuantities);
    }

    /** True once an outcome is recorded (COLLECTED or NOT_COLLECTED). */
    public boolean isTerminal() {
        return status == TourStopStatus.COLLECTED || status == TourStopStatus.NOT_COLLECTED;
    }

    /** PENDING to COLLECTED (D-031). Quantities keep insertion order; the note may be null. */
    public void recordCollected(Map<WasteCategory, BigDecimal> quantities, String incidentNote,
            AppUser recordedBy, Instant recordedAt) {
        requireRecordable(recordedBy, recordedAt);
        if (quantities == null || quantities.isEmpty()) {
            throw new IllegalArgumentException("A collected stop needs at least one quantity.");
        }
        for (BigDecimal q : quantities.values()) {
            if (q == null || q.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("A collected quantity must be zero or positive.");
            }
        }
        quantities.forEach((category, q) -> actualQuantities.add(new CollectedQuantity(this, category, q)));
        this.incidentReason = blankToNull(incidentNote);
        stamp(TourStopStatus.COLLECTED, recordedBy, recordedAt);
    }

    /** PENDING to NOT_COLLECTED with a non-blank reason (D-031). */
    public void recordNotCollected(String reason, AppUser recordedBy, Instant recordedAt) {
        requireRecordable(recordedBy, recordedAt);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A not-collected stop needs a reason.");
        }
        this.incidentReason = reason.trim();
        stamp(TourStopStatus.NOT_COLLECTED, recordedBy, recordedAt);
    }

    private void requireRecordable(AppUser recordedBy, Instant recordedAt) {
        if (tour.getStatus() != TourStatus.IN_PROGRESS) {
            throw new IllegalStateException("Outcomes can be recorded only on an IN_PROGRESS tour.");
        }
        if (status != TourStopStatus.PENDING) {
            throw new IllegalStateException("The stop already has the outcome " + status + ".");
        }
        if (recordedBy == null || recordedAt == null) {
            throw new IllegalArgumentException("An outcome needs its actor and time.");
        }
    }

    private void stamp(TourStopStatus newStatus, AppUser recordedBy, Instant recordedAt) {
        this.status = newStatus;
        this.outcomeRecordedBy = recordedBy;
        this.outcomeRecordedAt = recordedAt;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Sum of the actual quantities (kg); ZERO when none. */
    public BigDecimal actualTotalKg() {
        return actualQuantities.stream()
                .map(CollectedQuantity::getActualQuantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sum of the planned quantities over all categories (kg). */
    public BigDecimal plannedTotalKg() {
        return stopDemands.stream()
                .map(StopDemand::getPlannedQuantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
