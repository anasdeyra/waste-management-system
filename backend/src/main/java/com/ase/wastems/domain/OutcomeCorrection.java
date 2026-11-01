package com.ase.wastems.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A retained correction of a recorded stop outcome (US-31, D-027): the previous and the corrected
 * values, who corrected it, when and why. Never edited or deleted.
 */
@Entity
@Table(name = "outcome_correction")
public class OutcomeCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tour_stop_id")
    private TourStop tourStop;

    @Column(name = "corrected_at", nullable = false)
    private Instant correctedAt;

    @Column(name = "corrected_by", nullable = false, length = 50)
    private String correctedBy;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private TourStopStatus previousStatus;

    @Column(name = "previous_incident_reason", length = 500)
    private String previousIncidentReason;

    @Column(name = "previous_recorded_by", length = 50)
    private String previousRecordedBy;

    @Column(name = "previous_recorded_at")
    private Instant previousRecordedAt;

    @ElementCollection
    @CollectionTable(
            name = "outcome_correction_previous_quantity",
            joinColumns = @JoinColumn(name = "correction_id"))
    @OrderColumn(name = "position")
    private List<CorrectionQuantity> previousQuantities = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private TourStopStatus newStatus;

    @Column(name = "new_incident_reason", length = 500)
    private String newIncidentReason;

    @ElementCollection
    @CollectionTable(
            name = "outcome_correction_new_quantity",
            joinColumns = @JoinColumn(name = "correction_id"))
    @OrderColumn(name = "position")
    private List<CorrectionQuantity> newQuantities = new ArrayList<>();

    protected OutcomeCorrection() {}

    OutcomeCorrection(
            TourStop stop,
            Instant correctedAt,
            String correctedBy,
            String reason,
            TourStopStatus previousStatus,
            String previousIncidentReason,
            String previousRecordedBy,
            Instant previousRecordedAt,
            List<CorrectionQuantity> previousQuantities,
            TourStopStatus newStatus,
            String newIncidentReason,
            List<CorrectionQuantity> newQuantities) {
        this.tourStop = stop;
        this.correctedAt = correctedAt;
        this.correctedBy = correctedBy;
        this.reason = reason;
        this.previousStatus = previousStatus;
        this.previousIncidentReason = previousIncidentReason;
        this.previousRecordedBy = previousRecordedBy;
        this.previousRecordedAt = previousRecordedAt;
        this.previousQuantities.addAll(previousQuantities);
        this.newStatus = newStatus;
        this.newIncidentReason = newIncidentReason;
        this.newQuantities.addAll(newQuantities);
    }

    public UUID getId() {
        return id;
    }

    public TourStop getTourStop() {
        return tourStop;
    }

    public Instant getCorrectedAt() {
        return correctedAt;
    }

    public String getCorrectedBy() {
        return correctedBy;
    }

    public String getReason() {
        return reason;
    }

    public TourStopStatus getPreviousStatus() {
        return previousStatus;
    }

    public String getPreviousIncidentReason() {
        return previousIncidentReason;
    }

    public String getPreviousRecordedBy() {
        return previousRecordedBy;
    }

    public Instant getPreviousRecordedAt() {
        return previousRecordedAt;
    }

    public List<CorrectionQuantity> getPreviousQuantities() {
        return Collections.unmodifiableList(previousQuantities);
    }

    public TourStopStatus getNewStatus() {
        return newStatus;
    }

    public String getNewIncidentReason() {
        return newIncidentReason;
    }

    public List<CorrectionQuantity> getNewQuantities() {
        return Collections.unmodifiableList(newQuantities);
    }
}
