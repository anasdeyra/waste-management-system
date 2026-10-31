package com.ase.wastems.web.dto;

import com.ase.wastems.domain.WasteCategory;

/** Read model of a waste category returned by the REST API. */
public record WasteCategoryResponse(Long id, String name, String description, boolean recyclable) {

    public static WasteCategoryResponse from(WasteCategory category) {
        return new WasteCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isRecyclable());
    }
}
