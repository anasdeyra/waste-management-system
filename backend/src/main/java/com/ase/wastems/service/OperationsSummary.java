package com.ase.wastems.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Recorded operational counts for a planning period (US-35); not live tracking. */
public record OperationsSummary(LocalDate from, LocalDate to, Instant generatedAt, ResourceCounts vehicles,
        ResourceCounts agents, PointCounts collectionPoints, TourCounts tours, StopCounts stops,
        BigDecimal collectedKg) {

    public record ResourceCounts(long total, long available, long unavailable) {}

    public record PointCounts(long total, long active, long inactive) {}

    public record TourCounts(long total, long draft, long planned, long inProgress, long completed,
            long cancelled) {}

    public record StopCounts(long total, long pending, long collected, long notCollected) {}
}
