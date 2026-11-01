package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * A collection vehicle with a single kilogram capacity (D-024). Never deleted; made unavailable
 * instead, so tour assignments keep its identity.
 */
@Entity
@Table(
        name = "vehicle",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_vehicle_registration", columnNames = "registration"))
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String registration;

    @Column(name = "capacity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceStatus status = ResourceStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataOrigin origin;

    protected Vehicle() {}

    public Vehicle(String registration, BigDecimal capacityKg, DataOrigin origin) {
        this.registration = registration;
        this.capacityKg = capacityKg;
        this.origin = origin;
    }

    public UUID getId() {
        return id;
    }

    public String getRegistration() {
        return registration;
    }

    public void setRegistration(String registration) {
        this.registration = registration;
    }

    public BigDecimal getCapacityKg() {
        return capacityKg;
    }

    public void setCapacityKg(BigDecimal capacityKg) {
        this.capacityKg = capacityKg;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public DataOrigin getOrigin() {
        return origin;
    }

    public boolean isAvailable() {
        return status == ResourceStatus.AVAILABLE;
    }
}
