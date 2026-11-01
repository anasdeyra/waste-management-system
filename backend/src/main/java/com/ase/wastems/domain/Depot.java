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
import java.util.UUID;

/** A vehicle depot (WGS84 location). Read-only in S2. */
@Entity
@Table(
        name = "depot",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_depot_source",
                        columnNames = {"source_dataset", "source_identifier"}))
public class Depot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double latitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataOrigin origin;

    @Column(name = "source_dataset", length = 80)
    private String sourceDataset;

    @Column(name = "source_identifier", length = 80)
    private String sourceIdentifier;

    protected Depot() {}

    public Depot(
            String name,
            double longitude,
            double latitude,
            DataOrigin origin,
            String sourceDataset,
            String sourceIdentifier) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.origin = origin;
        this.sourceDataset = sourceDataset;
        this.sourceIdentifier = sourceIdentifier;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public DataOrigin getOrigin() {
        return origin;
    }

    public String getSourceDataset() {
        return sourceDataset;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }
}
