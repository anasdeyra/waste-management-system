package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;

/** Current demand (kg) of one waste category at a collection point. */
@Entity
@Table(
        name = "demand_entry",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_demand_point_category",
                        columnNames = {"collection_point_id", "category_id"}))
public class DemandEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_point_id")
    private CollectionPoint collectionPoint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private WasteCategory category;

    @Column(name = "quantity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataOrigin origin;

    protected DemandEntry() {}

    public DemandEntry(WasteCategory category, BigDecimal quantityKg, DataOrigin origin) {
        this.category = category;
        this.quantityKg = quantityKg;
        this.origin = origin;
    }

    public UUID getId() {
        return id;
    }

    public CollectionPoint getCollectionPoint() {
        return collectionPoint;
    }

    void setCollectionPoint(CollectionPoint collectionPoint) {
        this.collectionPoint = collectionPoint;
    }

    public WasteCategory getCategory() {
        return category;
    }

    public void setCategory(WasteCategory category) {
        this.category = category;
    }

    public BigDecimal getQuantityKg() {
        return quantityKg;
    }

    public void setQuantityKg(BigDecimal quantityKg) {
        this.quantityKg = quantityKg;
    }

    public DataOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(DataOrigin origin) {
        this.origin = origin;
    }
}
