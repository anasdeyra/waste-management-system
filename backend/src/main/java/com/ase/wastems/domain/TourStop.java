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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A collection point selected in a tour, with its explicit input/visit order and its planned
 * demand snapshot. Outcome fields arrive in S7.
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

    /** Sum of the planned quantities over all categories (kg). */
    public BigDecimal plannedTotalKg() {
        return stopDemands.stream()
                .map(StopDemand::getPlannedQuantityKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
