package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Adds a collection point as the last stop of a tour. */
public record AddTourStopRequest(@NotNull UUID collectionPointId) {}
