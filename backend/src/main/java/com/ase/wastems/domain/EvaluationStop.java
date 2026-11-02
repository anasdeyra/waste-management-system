package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Frozen stop of an evaluation scenario: name, coordinates and demand as they were when the
 * scenario was built. Evaluation provenance record, outside the operational class diagram (D-033).
 */
@Embeddable
public class EvaluationStop {

    @Column(name = "input_index", nullable = false)
    private int inputIndex;

    @Column(name = "collection_point_id", nullable = false)
    private UUID collectionPointId;

    @Column(name = "source_dataset", length = 40)
    private String sourceDataset;

    @Column(name = "source_identifier", length = 40)
    private String sourceIdentifier;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "demand_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal demandKg;

    @Column(name = "demand_breakdown", length = 500)
    private String demandBreakdown;

    protected EvaluationStop() {}

    public EvaluationStop(int inputIndex, UUID collectionPointId, String sourceDataset,
            String sourceIdentifier, String name, double longitude, double latitude, BigDecimal demandKg) {
        this(inputIndex, collectionPointId, sourceDataset, sourceIdentifier, name, longitude, latitude, demandKg,
                null);
    }

    public EvaluationStop(int inputIndex, UUID collectionPointId, String sourceDataset,
            String sourceIdentifier, String name, double longitude, double latitude, BigDecimal demandKg,
            String demandBreakdown) {
        this.demandBreakdown = demandBreakdown;
        this.inputIndex = inputIndex;
        this.collectionPointId = collectionPointId;
        this.sourceDataset = sourceDataset;
        this.sourceIdentifier = sourceIdentifier;
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.demandKg = demandKg;
    }

    public int getInputIndex() {
        return inputIndex;
    }

    public UUID getCollectionPointId() {
        return collectionPointId;
    }

    public String getSourceDataset() {
        return sourceDataset;
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public String getName() {
        return name;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public BigDecimal getDemandKg() {
        return demandKg;
    }

    public String getDemandBreakdown() {
        return demandBreakdown;
    }
}
