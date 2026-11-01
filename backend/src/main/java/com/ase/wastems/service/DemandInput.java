package com.ase.wastems.service;

import java.math.BigDecimal;
import java.util.UUID;

/** Service-level demand input, independent of the web DTOs. */
public record DemandInput(UUID categoryId, BigDecimal quantityKg) {}
