package com.ase.wastems.domain;

/**
 * Validation findings of a tour: structural issues of the draft (S4) and the additional
 * confirmation issues (S6).
 */
public enum TourIssueCode {
    NO_STOPS,
    NO_VEHICLE,
    NO_DRIVER,
    VEHICLE_UNAVAILABLE,
    DRIVER_UNAVAILABLE,
    COLLECTOR_UNAVAILABLE,
    INACTIVE_STOP,
    INELIGIBLE_STOP,
    OVER_CAPACITY,
    PLANNING_DATE_PAST,
    ROUTE_MISSING,
    ROUTE_OUTDATED
}
