package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/** Create or header-edit request of a tour draft. */
public record TourRequest(@NotNull LocalDate planningDate, @NotNull UUID depotId) {}
