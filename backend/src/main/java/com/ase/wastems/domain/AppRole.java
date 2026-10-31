package com.ase.wastems.domain;

/** Role of an application user, used for authorization (FR-02). */
public enum AppRole {
    /** Manages users and reference data. */
    ADMIN,
    /** Plans tours and manages operational resources. */
    MANAGER,
    /** Executes tours and records collection results. */
    AGENT
}
