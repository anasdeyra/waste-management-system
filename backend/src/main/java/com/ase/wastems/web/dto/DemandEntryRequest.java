package com.ase.wastems.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** One demand entry (category and kilograms) of a create or edit request. */
public record DemandEntryRequest(
        @NotNull UUID categoryId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2)
                BigDecimal quantityKg) {}
