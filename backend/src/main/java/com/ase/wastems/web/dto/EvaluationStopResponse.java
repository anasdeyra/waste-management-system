package com.ase.wastems.web.dto;

import com.ase.wastems.domain.EvaluationStop;
import java.math.BigDecimal;
import java.util.UUID;

/** Stop as frozen in an evaluation run. */
public record EvaluationStopResponse(int inputIndex, UUID collectionPointId, String sourceDataset,
        String sourceIdentifier, String name, double longitude, double latitude, BigDecimal demandKg) {

    public static EvaluationStopResponse from(EvaluationStop s) {
        return new EvaluationStopResponse(s.getInputIndex(), s.getCollectionPointId(), s.getSourceDataset(),
                s.getSourceIdentifier(), s.getName(), s.getLongitude(), s.getLatitude(), s.getDemandKg());
    }
}
