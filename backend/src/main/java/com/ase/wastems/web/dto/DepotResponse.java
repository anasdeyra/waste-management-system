package com.ase.wastems.web.dto;

import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.Depot;
import java.util.UUID;

/** A depot as seen by the client. */
public record DepotResponse(
        UUID id,
        String name,
        double longitude,
        double latitude,
        DataOrigin origin,
        String sourceDataset,
        String sourceIdentifier) {

    public static DepotResponse from(Depot d) {
        return new DepotResponse(
                d.getId(),
                d.getName(),
                d.getLongitude(),
                d.getLatitude(),
                d.getOrigin(),
                d.getSourceDataset(),
                d.getSourceIdentifier());
    }
}
