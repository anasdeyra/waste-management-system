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

/** A single stop of a tour: one collection point visited at a given position. */
@Entity
@Table(name = "tour_stop")
public class TourStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Position of the stop inside the tour, starting at 1. */
    @Column(nullable = false)
    private int sequence;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "collection_point_id", nullable = false)
    private CollectionPoint collectionPoint;

    /** Expected kilograms to collect at this stop. */
    @Column(name = "expected_kg", nullable = false)
    private double expectedKg;

    /** Actually collected kilograms; {@code null} until the stop is executed. */
    @Column(name = "collected_kg")
    private Double collectedKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TourStopStatus status = TourStopStatus.PENDING;

    protected TourStop() {
        // Required by JPA.
    }

    public TourStop(int sequence, CollectionPoint collectionPoint, double expectedKg) {
        this.sequence = sequence;
        this.collectionPoint = collectionPoint;
        this.expectedKg = expectedKg;
    }

    public Long getId() {
        return id;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public Tour getTour() {
        return tour;
    }

    void setTour(Tour tour) {
        this.tour = tour;
    }

    public CollectionPoint getCollectionPoint() {
        return collectionPoint;
    }

    public void setCollectionPoint(CollectionPoint collectionPoint) {
        this.collectionPoint = collectionPoint;
    }

    public double getExpectedKg() {
        return expectedKg;
    }

    public void setExpectedKg(double expectedKg) {
        this.expectedKg = expectedKg;
    }

    public Double getCollectedKg() {
        return collectedKg;
    }

    public void setCollectedKg(Double collectedKg) {
        this.collectedKg = collectedKg;
    }

    public TourStopStatus getStatus() {
        return status;
    }

    public void setStatus(TourStopStatus status) {
        this.status = status;
    }
}
