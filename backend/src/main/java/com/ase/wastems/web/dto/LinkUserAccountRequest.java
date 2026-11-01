package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request to link an operational agent to an AGENT account. */
public record LinkUserAccountRequest(@NotNull UUID userId) {}
