package com.ase.wastems.web.dto;

import com.ase.wastems.domain.WasteCategory;
import java.util.UUID;

/** A waste category as seen by the client. */
public record WasteCategoryResponse(UUID id, String name, String description, boolean active) {

    public static WasteCategoryResponse from(WasteCategory c) {
        return new WasteCategoryResponse(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }
}
