package com.ase.wastems.domain;

/**
 * Origin of an evaluation scenario. Evaluation provenance record, outside the operational class
 * diagram (D-033). S9 adds the seeded
 * experiment and CSV-imported scenarios.
 */
public enum ScenarioKind {
    TOUR_DRAFT,
    EXPERIMENT,
    CSV_IMPORT
}
