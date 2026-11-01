package com.ase.wastems.domain;

/** Tour lifecycle (D-025); S4 creates only DRAFT tours. */
public enum TourStatus {
    DRAFT,
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
