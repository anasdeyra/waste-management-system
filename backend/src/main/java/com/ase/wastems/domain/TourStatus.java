package com.ase.wastems.domain;

/** Lifecycle of a collection tour. */
public enum TourStatus {
    /** The tour is planned but not started. */
    PLANNED,
    /** The tour is being executed. */
    IN_PROGRESS,
    /** The tour is finished and its results are recorded. */
    COMPLETED,
    /** The tour was cancelled before or during execution. */
    CANCELLED
}
