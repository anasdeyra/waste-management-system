package com.ase.wastems.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** A waste collection point at a WGS84 location with its current demand entries. */
@Entity
@Table(
        name = "collection_point",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_point_source",
                        columnNames = {"source_dataset", "source_identifier"}))
public class CollectionPoint {

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
    private CollectionPointStatus status = CollectionPointStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataOrigin origin;

    @Column(name = "source_dataset", length = 80)
    private String sourceDataset;

    @Column(name = "source_identifier", length = 80)
    private String sourceIdentifier;

    @Column(name = "source_ownership_type", length = 80)
    private String sourceOwnershipType;

    @OneToMany(mappedBy = "collectionPoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DemandEntry> demands = new ArrayList<>();

    protected CollectionPoint() {}

    public CollectionPoint(String name, double longitude, double latitude, DataOrigin origin) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.origin = origin;
    }

    public void setSource(String dataset, String identifier, String ownershipType) {
        this.sourceDataset = dataset;
        this.sourceIdentifier = identifier;
        this.sourceOwnershipType = ownershipType;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public CollectionPointStatus getStatus() {
        return status;
    }

    public void setStatus(CollectionPointStatus status) {
        this.status = status;
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

    public String getSourceOwnershipType() {
        return sourceOwnershipType;
    }

    public List<DemandEntry> getDemands() {
        return Collections.unmodifiableList(demands);
    }

    public void addDemand(DemandEntry demand) {
        demand.setCollectionPoint(this);
        demands.add(demand);
    }

    public void removeDemand(DemandEntry demand) {
        demands.remove(demand);
        demand.setCollectionPoint(null);
    }

    public boolean isActive() {
        return status == CollectionPointStatus.ACTIVE;
    }

    /** Sum of the demand entries over all waste categories (kg). */
    public BigDecimal totalDemandKg() {
        return demands.stream().map(DemandEntry::getQuantityKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
