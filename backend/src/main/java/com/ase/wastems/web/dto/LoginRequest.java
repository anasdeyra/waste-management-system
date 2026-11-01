package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials submitted to sign in. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
