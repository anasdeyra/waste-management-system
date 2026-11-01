package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/tours/{id}/cancellation} (US-30). */
public record CancelTourRequest(@NotBlank @Size(max = 500) String reason) {}
