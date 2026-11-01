package com.ase.wastems.service;

import java.math.BigDecimal;
import java.util.UUID;

/** Stop as frozen in an evaluation scenario: point identity, name, coordinates and demand total. */
public record FrozenStop(int inputIndex, UUID collectionPointId, String sourceDataset, String sourceIdentifier,
        String name, double longitude, double latitude, BigDecimal demandKg) {}
