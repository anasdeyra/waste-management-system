package com.ase.wastems.web.dto;

import com.ase.wastems.domain.CollectionPoint;

/** Read model of a collection point returned by the REST API. */
public record CollectionPointResponse(
        Long id,
        String name,
        String address,
        double x,
        double y,
        double capacityKg,
        double currentLoadKg,
        String status,
        Long wasteCategoryId,
        String wasteCategoryName) {

    public static CollectionPointResponse from(CollectionPoint point) {
        return new CollectionPointResponse(
                point.getId(),
                point.getName(),
                point.getAddress(),
                point.getX(),
                point.getY(),
                point.getCapacityKg(),
                point.getCurrentLoadKg(),
                point.getStatus().name(),
                point.getWasteCategory().getId(),
                point.getWasteCategory().getName());
    }
}
