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
 * Actual quantity of one waste category collected at a stop (D-031, OCL
 * {@code NonNegativeActualQuantity}).
 */
@Entity
@Table(
        name = "collected_quantity",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_collected_quantity_category",
                        columnNames = {"tour_stop_id", "category_id"}))
public class CollectedQuantity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tour_stop_id")
    private TourStop tourStop;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private WasteCategory category;

    @Column(name = "actual_quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualQuantityKg;

    protected CollectedQuantity() {}

    CollectedQuantity(TourStop tourStop, WasteCategory category, BigDecimal actualQuantityKg) {
        this.tourStop = tourStop;
        this.category = category;
        this.actualQuantityKg = actualQuantityKg;
    }

    /** In-place correction of the kilograms (US-31); keeps the row and its unique key. */
    void setActualQuantityKg(BigDecimal kg) {
        if (kg == null || kg.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("A collected quantity must be zero or positive.");
        }
        this.actualQuantityKg = kg;
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

    public BigDecimal getActualQuantityKg() {
        return actualQuantityKg;
    }
}
