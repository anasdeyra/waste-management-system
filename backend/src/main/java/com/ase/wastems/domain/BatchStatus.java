package com.ase.wastems.domain;

/** Lifecycle of an evaluation batch (experiment execution or CSV import). */
public enum BatchStatus {
    RUNNING,
    COMPLETED,
    ABORTED
}
