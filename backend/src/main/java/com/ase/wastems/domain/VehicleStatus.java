package com.ase.wastems.domain;

/** Operational availability of a collection vehicle. */
public enum VehicleStatus {
    /** The vehicle can be assigned to a tour. */
    AVAILABLE,
    /** The vehicle is currently executing a tour. */
    IN_SERVICE,
    /** The vehicle is under maintenance and cannot be assigned. */
    MAINTENANCE
}
