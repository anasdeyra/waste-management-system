package com.ase.wastems.traffic;

/** Classification counts of a snapshot; invalid links are counted but never drawn. */
public record TrafficCounts(int total, int observed, int stale, int noData, int invalid) {}
