package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Planned demand of one waste category at a tour stop: a snapshot of the point's DemandEntry
 * taken when the stop was added (class diagram §1), so later demand edits do not rewrite the
 * tour.
 */
@Entity
@Table(
        name = "stop_demand",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_stop_demand_category",
                        columnNames = {"tour_stop_id", "category_id"}))
public class StopDemand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tour_stop_id")
    private TourStop tourStop;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private WasteCategory category;

    @Column(name = "planned_quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal plannedQuantityKg;

    protected StopDemand() {}

    StopDemand(TourStop tourStop, WasteCategory category, BigDecimal plannedQuantityKg) {
        this.tourStop = tourStop;
        this.category = category;
        this.plannedQuantityKg = plannedQuantityKg;
    }

    public UUID getId() {
        return id;
    }

    public TourStop getTourStop() {
        return tourStop;
    }

    public WasteCategory getCategory() {
        return category;
    }

    public BigDecimal getPlannedQuantityKg() {
        return plannedQuantityKg;
    }
}
