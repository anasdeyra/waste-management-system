package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials submitted by a user to the login endpoint. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
