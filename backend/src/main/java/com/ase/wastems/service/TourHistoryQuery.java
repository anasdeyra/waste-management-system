package com.ase.wastems.service;

import com.ase.wastems.domain.TourStatus;
import java.time.LocalDate;
import java.util.UUID;

/** Filters of a history search (US-32); a null component means no filter. */
public record TourHistoryQuery(
        LocalDate from, LocalDate to, TourStatus status, UUID vehicleId, UUID agentId, UUID pointId) {}
