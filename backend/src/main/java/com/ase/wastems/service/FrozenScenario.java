package com.ase.wastems.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Immutable input shared by all three methods (US-21, NFR-07). */
public record FrozenScenario(UUID tourId, long tourRevision, FrozenDepot depot, List<FrozenStop> stops,
        BigDecimal vehicleCapacityKg, List<String> sourceSnapshotIds) {

    public FrozenScenario {
        stops = List.copyOf(stops);
        sourceSnapshotIds = List.copyOf(sourceSnapshotIds);
    }
}
