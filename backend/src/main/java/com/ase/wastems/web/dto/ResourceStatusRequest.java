package com.ase.wastems.web.dto;

import com.ase.wastems.domain.ResourceStatus;
import jakarta.validation.constraints.NotNull;

/** Availability change of a vehicle or an agent. */
public record ResourceStatusRequest(@NotNull ResourceStatus status) {}
