package com.ase.wastems.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create or update request of a waste category. */
public record WasteCategoryRequest(
        @NotBlank @Size(max = 80) String name, @Size(max = 500) String description) {}
