package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotNull;

/** Activation or deactivation request, shared by users and waste categories. */
public record ActiveStatusRequest(@NotNull Boolean active) {}
