package com.ase.wastems.service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stop as frozen in an evaluation scenario: point identity, name, coordinates, demand total and,
 * for experiment and CSV scenarios, the per-category demand breakdown.
 */
public record FrozenStop(int inputIndex, UUID collectionPointId, String sourceDataset, String sourceIdentifier,
        String name, double longitude, double latitude, BigDecimal demandKg, String demandBreakdown) {

    public FrozenStop(int inputIndex, UUID collectionPointId, String sourceDataset, String sourceIdentifier,
            String name, double longitude, double latitude, BigDecimal demandKg) {
        this(inputIndex, collectionPointId, sourceDataset, sourceIdentifier, name, longitude, latitude, demandKg,
                null);
    }
}
