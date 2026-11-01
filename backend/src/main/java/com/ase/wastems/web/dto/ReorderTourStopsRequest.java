package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** The complete new stop order, as stop ids. */
public record ReorderTourStopsRequest(@NotNull List<@NotNull UUID> stopIds) {}
