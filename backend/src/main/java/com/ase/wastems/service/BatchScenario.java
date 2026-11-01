package com.ase.wastems.service;

import java.math.BigDecimal;
import java.util.List;

/** Frozen scenario of an experiment or a CSV import, evaluated by the three methods. */
public record BatchScenario(String scenarioKey, String demandProfile, Long seed, Integer repetition,
        FrozenDepot depot, List<FrozenStop> stops, BigDecimal vehicleCapacityKg, List<String> sourceSnapshotIds) {

    public BatchScenario {
        stops = List.copyOf(stops);
        sourceSnapshotIds = List.copyOf(sourceSnapshotIds);
    }
}
