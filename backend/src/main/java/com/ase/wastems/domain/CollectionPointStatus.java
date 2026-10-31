package com.ase.wastems.domain;

/** Lifecycle of a collection point. */
public enum CollectionPointStatus {
    /** The point is part of the planning scope and can be collected. */
    ACTIVE,
    /** The point is temporarily out of service (e.g. blocked access). */
    INACTIVE,
    /** The point reached its capacity and waits for an unscheduled collection. */
    FULL
}
