package com.ase.wastems.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Actual quantity of one waste category found at a stop. */
public record CollectedQuantityRequest(
        @NotNull UUID categoryId,
        @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal actualQuantityKg) {}
