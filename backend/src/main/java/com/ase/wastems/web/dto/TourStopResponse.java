package com.ase.wastems.web.dto;

import com.ase.wastems.domain.TourStop;

/** Read model of a single stop of a tour. */
public record TourStopResponse(
        Long id,
        int sequence,
        Long collectionPointId,
        String collectionPointName,
        double expectedKg,
        Double collectedKg,
        String status) {

    public static TourStopResponse from(TourStop stop) {
        return new TourStopResponse(
                stop.getId(),
                stop.getSequence(),
                stop.getCollectionPoint().getId(),
                stop.getCollectionPoint().getName(),
                stop.getExpectedKg(),
                stop.getCollectedKg(),
                stop.getStatus().name());
    }
}
