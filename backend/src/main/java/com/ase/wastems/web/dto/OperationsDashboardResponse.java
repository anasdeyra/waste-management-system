package com.ase.wastems.web.dto;

import com.ase.wastems.service.OperationsSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Operational dashboard (US-35): recorded data, not live tracking; from/to omitted when absent. */
public record OperationsDashboardResponse(LocalDate from, LocalDate to, Instant generatedAt,
        ResourceCountsResponse vehicles, ResourceCountsResponse agents, PointCountsResponse collectionPoints,
        TourCountsResponse tours, StopCountsResponse stops, BigDecimal collectedKg) {

    public record ResourceCountsResponse(long total, long available, long unavailable) {}

    public record PointCountsResponse(long total, long active, long inactive) {}

    public record TourCountsResponse(long total, long draft, long planned, long inProgress, long completed,
            long cancelled) {}

    public record StopCountsResponse(long total, long pending, long collected, long notCollected) {}

    public static OperationsDashboardResponse from(OperationsSummary s) {
        return new OperationsDashboardResponse(s.from(), s.to(), s.generatedAt(),
                new ResourceCountsResponse(s.vehicles().total(), s.vehicles().available(), s.vehicles().unavailable()),
                new ResourceCountsResponse(s.agents().total(), s.agents().available(), s.agents().unavailable()),
                new PointCountsResponse(s.collectionPoints().total(), s.collectionPoints().active(),
                        s.collectionPoints().inactive()),
                new TourCountsResponse(s.tours().total(), s.tours().draft(), s.tours().planned(),
                        s.tours().inProgress(), s.tours().completed(), s.tours().cancelled()),
                new StopCountsResponse(s.stops().total(), s.stops().pending(), s.stops().collected(),
                        s.stops().notCollected()),
                s.collectedKg());
    }
}
