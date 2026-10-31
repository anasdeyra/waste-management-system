package com.ase.wastems.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A planned collection tour: one vehicle, one driver, optional collectors and an ordered list
 * of stops (FR-11).
 */
@Entity
@Table(name = "tour")
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "planned_date", nullable = false)
    private LocalDate plannedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TourStatus status = TourStatus.PLANNED;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Agent driver;

    /**
     * Optional collectors of the crew (D-011): a tour has one driver and zero to two
     * collectors.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tour_collector",
            joinColumns = @JoinColumn(name = "tour_id"),
            inverseJoinColumns = @JoinColumn(name = "agent_id"))
    private Set<Agent> collectors = new LinkedHashSet<>();

    /** Total travelled distance of the tour, in kilometres. */
    @Column(name = "total_distance_km", nullable = false)
    private double totalDistanceKm;

    /** Estimated execution duration of the tour, in minutes. */
    @Column(name = "estimated_duration_minutes", nullable = false)
    private double estimatedDurationMinutes;

    /** Total collected load of the tour, in kilograms. */
    @Column(name = "total_load_kg", nullable = false)
    private double totalLoadKg;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<TourStop> stops = new ArrayList<>();

    protected Tour() {
        // Required by JPA.
    }

    public Tour(LocalDate plannedDate, Vehicle vehicle, Agent driver) {
        this.plannedDate = plannedDate;
        this.vehicle = vehicle;
        this.driver = driver;
    }

    /** Adds a stop at the end of the tour and keeps the back-reference consistent. */
    public void addStop(TourStop stop) {
        stop.setTour(this);
        this.stops.add(stop);
    }

    public Long getId() {
        return id;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public TourStatus getStatus() {
        return status;
    }

    public void setStatus(TourStatus status) {
        this.status = status;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Agent getDriver() {
        return driver;
    }

    public void setDriver(Agent driver) {
        this.driver = driver;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = totalDistanceKm;
    }

    public double getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(double estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public double getTotalLoadKg() {
        return totalLoadKg;
    }

    public void setTotalLoadKg(double totalLoadKg) {
        this.totalLoadKg = totalLoadKg;
    }

    public List<TourStop> getStops() {
        return stops;
    }

    public Set<Agent> getCollectors() {
        return collectors;
    }
}
