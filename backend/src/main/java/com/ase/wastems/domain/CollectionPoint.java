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

/**
 * A geographically located place where waste is deposited and collected.
 *
 * <p>The planning coordinates are expressed in kilometres on a synthetic Cartesian plane, as
 * decided in D-009. They are not geographic coordinates: the prototype deliberately avoids
 * external mapping services.
 */
@Entity
@Table(name = "collection_point")
public class CollectionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(name = "coord_x", nullable = false)
    private double x;

    @Column(name = "coord_y", nullable = false)
    private double y;

    /** Maximum amount of waste the point can hold, in kilograms. */
    @Column(name = "capacity_kg", nullable = false)
    private double capacityKg;

    /** Amount of waste currently stored at the point, in kilograms. */
    @Column(name = "current_load_kg", nullable = false)
    private double currentLoadKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CollectionPointStatus status = CollectionPointStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "waste_category_id", nullable = false)
    private WasteCategory wasteCategory;

    protected CollectionPoint() {
        // Required by JPA.
    }

    public CollectionPoint(
            String name,
            String address,
            double x,
            double y,
            double capacityKg,
            double currentLoadKg,
            WasteCategory wasteCategory) {
        this.name = name;
        this.address = address;
        this.x = x;
        this.y = y;
        this.capacityKg = capacityKg;
        this.currentLoadKg = currentLoadKg;
        this.wasteCategory = wasteCategory;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getCapacityKg() {
        return capacityKg;
    }

    public void setCapacityKg(double capacityKg) {
        this.capacityKg = capacityKg;
    }

    public double getCurrentLoadKg() {
        return currentLoadKg;
    }

    public void setCurrentLoadKg(double currentLoadKg) {
        this.currentLoadKg = currentLoadKg;
    }

    public CollectionPointStatus getStatus() {
        return status;
    }

    public void setStatus(CollectionPointStatus status) {
        this.status = status;
    }

    public WasteCategory getWasteCategory() {
        return wasteCategory;
    }

    public void setWasteCategory(WasteCategory wasteCategory) {
        this.wasteCategory = wasteCategory;
    }
}
