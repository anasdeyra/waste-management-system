package com.ase.wastems.web.dto;

import com.ase.wastems.domain.CollectionPointStatus;
import jakarta.validation.constraints.NotNull;

/** Status change request of a collection point. */
public record CollectionPointStatusRequest(@NotNull CollectionPointStatus status) {}
