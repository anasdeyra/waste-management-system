package com.ase.wastems.traffic;

/** Reading state of a drawn sensor link; unusable links are counted as invalid and never drawn. */
public enum TrafficLinkStatus {
    OBSERVED,
    STALE,
    NO_DATA
}
