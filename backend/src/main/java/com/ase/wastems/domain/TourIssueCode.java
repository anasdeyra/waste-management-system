package com.ase.wastems.domain;

/** Structural validation findings of a draft (activity diagram "structurally valid?"). */
public enum TourIssueCode {
    NO_STOPS,
    NO_VEHICLE,
    NO_DRIVER,
    VEHICLE_UNAVAILABLE,
    DRIVER_UNAVAILABLE,
    COLLECTOR_UNAVAILABLE,
    INACTIVE_STOP,
    INELIGIBLE_STOP,
    OVER_CAPACITY
}
