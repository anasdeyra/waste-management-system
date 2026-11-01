package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create or edit request of an agent. */
public record AgentRequest(@NotBlank @Size(max = 100) String displayName) {}
