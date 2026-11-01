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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A collection point selected in a tour, with its explicit input/visit order and its planned
 * demand snapshot. S7 adds the outcome (D-031): COLLECTED with the actual quantity per waste
 * category, or NOT_COLLECTED with a reason, plus the recording actor and time. S8 adds retained
 * privileged corrections.
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

    @OneToMany(mappedBy = "tourStop", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("correctedAt ASC")
    private List<OutcomeCorrection> corrections = new ArrayList<>();

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

    /** Retained corrections, oldest first. */
    public List<OutcomeCorrection> getCorrections() {
        return Collections.unmodifiableList(corrections);
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

    /**
     * True iff the stop already has exactly this outcome: same status, same incident text (trimmed,
     * blank = null) and the same quantity per category id (compareTo == 0, same key set).
     */
    public boolean hasOutcome(
            TourStopStatus newStatus, Map<WasteCategory, BigDecimal> quantities, String incident) {
        if (status != newStatus) {
            return false;
        }
        String a = blankToNull(incidentReason);
        String b = blankToNull(incident);
        if (a == null ? b != null : !a.equals(b)) {
            return false;
        }
        Map<UUID, BigDecimal> current = new HashMap<>();
        actualQuantities.forEach(q -> current.put(q.getCategory().getId(), q.getActualQuantityKg()));
        Map<UUID, BigDecimal> wanted = new HashMap<>();
        if (quantities != null) {
            quantities.forEach((c, kg) -> wanted.put(c.getId(), kg));
        }
        if (!current.keySet().equals(wanted.keySet())) {
            return false;
        }
        return current.entrySet().stream()
                .allMatch(e -> e.getValue().compareTo(wanted.get(e.getKey())) == 0);
    }

    /**
     * Privileged correction of a recorded outcome (US-31): retains the previous values in a new
     * OutcomeCorrection, then applies the corrected status, quantities and incident text.
     * outcomeRecordedBy/At keep the original recording.
     */
    public OutcomeCorrection correctOutcome(
            TourStopStatus newStatus,
            Map<WasteCategory, BigDecimal> quantities,
            String incident,
            String reason,
            String correctedBy,
            Instant correctedAt) {
        if (!tour.isCorrectable()) {
            throw new IllegalStateException(
                    "Outcomes can be corrected only on an IN_PROGRESS, COMPLETED or CANCELLED tour.");
        }
        if (!isTerminal()) {
            throw new IllegalStateException("The stop has no recorded outcome to correct.");
        }
        if (newStatus != TourStopStatus.COLLECTED && newStatus != TourStopStatus.NOT_COLLECTED) {
            throw new IllegalArgumentException("A correction must result in COLLECTED or NOT_COLLECTED.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A correction needs a reason.");
        }
        if (correctedBy == null || correctedAt == null) {
            throw new IllegalArgumentException("A correction needs its actor and time.");
        }
        Map<WasteCategory, BigDecimal> wanted = quantities == null ? Map.of() : quantities;
        if (newStatus == TourStopStatus.COLLECTED) {
            if (wanted.isEmpty()) {
                throw new IllegalArgumentException("A collected stop needs at least one quantity.");
            }
            for (BigDecimal q : wanted.values()) {
                if (q == null || q.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("A collected quantity must be zero or positive.");
                }
            }
        } else {
            if (incident == null || incident.isBlank()) {
                throw new IllegalArgumentException("A not-collected stop needs a reason.");
            }
            if (!wanted.isEmpty()) {
                throw new IllegalArgumentException("A not-collected stop has no quantities.");
            }
        }
        // 1. snapshot of the previous values
        List<CorrectionQuantity> previous = actualQuantities.stream()
                .map(q -> new CorrectionQuantity(
                        q.getCategory().getId(), q.getCategory().getName(), q.getActualQuantityKg()))
                .sorted(Comparator.comparing(CorrectionQuantity::getCategoryName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        TourStopStatus previousStatus = status;
        String previousIncident = incidentReason;
        String previousBy = outcomeRecordedBy == null ? null : outcomeRecordedBy.getUsername();
        Instant previousAt = outcomeRecordedAt;
        // 2. quantities in place: update kept, remove dropped, add new (flush order, D-049)
        Map<UUID, BigDecimal> byId = new HashMap<>();
        wanted.forEach((c, kg) -> byId.put(c.getId(), kg));
        Set<UUID> present = new HashSet<>();
        for (Iterator<CollectedQuantity> it = actualQuantities.iterator(); it.hasNext(); ) {
            CollectedQuantity q = it.next();
            UUID cid = q.getCategory().getId();
            if (byId.containsKey(cid)) {
                q.setActualQuantityKg(byId.get(cid));
                present.add(cid);
            } else {
                it.remove();
            }
        }
        wanted.forEach((c, kg) -> {
            if (!present.contains(c.getId())) {
                actualQuantities.add(new CollectedQuantity(this, c, kg));
            }
        });
        // 3. status and incident text; the original recorder and time stay
        String newIncident = newStatus == TourStopStatus.COLLECTED ? blankToNull(incident) : incident.trim();
        this.incidentReason = newIncident;
        this.status = newStatus;
        // 4. the retained correction (strictly increasing time keeps the order stable)
        Instant at = correctedAt;
        if (!corrections.isEmpty()) {
            Instant last = corrections.get(corrections.size() - 1).getCorrectedAt();
            if (!at.isAfter(last)) {
                at = last.plusMillis(1);
            }
        }
        List<CorrectionQuantity> now = wanted.entrySet().stream()
                .map(e -> new CorrectionQuantity(e.getKey().getId(), e.getKey().getName(), e.getValue()))
                .sorted(Comparator.comparing(CorrectionQuantity::getCategoryName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        OutcomeCorrection c = new OutcomeCorrection(this, at, correctedBy, reason.trim(), previousStatus,
                previousIncident, previousBy, previousAt, previous, newStatus, newIncident, now);
        corrections.add(c);
        return c;
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
