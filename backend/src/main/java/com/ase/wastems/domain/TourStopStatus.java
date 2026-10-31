package com.ase.wastems.domain;

/** Execution status of a single stop inside a tour. */
public enum TourStopStatus {
    /** The stop has not been visited yet. */
    PENDING,
    /** The stop was visited and the collection was recorded. */
    DONE,
    /** The stop was visited but could not be collected (e.g. access denied). */
    SKIPPED
}
