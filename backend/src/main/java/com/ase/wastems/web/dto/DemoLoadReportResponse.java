package com.ase.wastems.web.dto;

import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.service.DemoCount;
import com.ase.wastems.service.DemoLoadReport;
import com.ase.wastems.service.DemoTourState;
import com.ase.wastems.service.DemoTrigger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Report of a demonstration load (US-33). */
public record DemoLoadReportResponse(
        DemoTrigger trigger,
        Instant loadedAt,
        DemoCountResponse categories,
        DemoCountResponse demandEntries,
        DemoCountResponse vehicles,
        DemoCountResponse accounts,
        DemoCountResponse agents,
        DemoCountResponse tours,
        List<DemoTourStateResponse> tourStates,
        List<String> warnings) {

    /** Created and already existing records of one type. */
    public record DemoCountResponse(int created, int existing) {
        static DemoCountResponse from(DemoCount c) {
            return new DemoCountResponse(c.created(), c.existing());
        }
    }

    /** One demonstration tour: key, target and current status. */
    public record DemoTourStateResponse(String key, UUID tourId, TourStatus targetStatus, TourStatus status) {
        static DemoTourStateResponse from(DemoTourState s) {
            return new DemoTourStateResponse(s.key(), s.tourId(), s.targetStatus(), s.status());
        }
    }

    public static DemoLoadReportResponse from(DemoLoadReport r) {
        return new DemoLoadReportResponse(
                r.trigger(),
                r.loadedAt(),
                DemoCountResponse.from(r.categories()),
                DemoCountResponse.from(r.demandEntries()),
                DemoCountResponse.from(r.vehicles()),
                DemoCountResponse.from(r.accounts()),
                DemoCountResponse.from(r.agents()),
                DemoCountResponse.from(r.tours()),
                r.tourStates().stream().map(DemoTourStateResponse::from).toList(),
                r.warnings());
    }
}
