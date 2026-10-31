package com.ase.wastems.web.dto;

/** Aggregated operational figures displayed on the dashboard. */
public record DashboardSummaryResponse(
        long collectionPoints,
        long activeCollectionPoints,
        long vehicles,
        long availableVehicles,
        long agents,
        long availableAgents,
        long toursPlannedToday,
        long toursInProgress,
        double collectedKg) {}
