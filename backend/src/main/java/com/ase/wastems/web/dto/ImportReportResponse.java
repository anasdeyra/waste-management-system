package com.ase.wastems.web.dto;

import com.ase.wastems.domain.SourceSnapshot;
import com.ase.wastems.service.DatasetImportResult;
import com.ase.wastems.service.ImportTrigger;
import com.ase.wastems.service.Mn01ImportReport;
import java.time.Instant;
import java.util.List;

/** An import run together with the current source snapshots. */
public record ImportReportResponse(
        ImportTrigger trigger,
        Instant startedAt,
        Instant finishedAt,
        DatasetImportResult baskets,
        DatasetImportResult depots,
        int syntheticDemandsCreated,
        String syntheticDemandCategory,
        List<String> warnings,
        List<SourceSnapshotResponse> snapshots) {

    public static ImportReportResponse from(Mn01ImportReport r, List<SourceSnapshot> snapshots) {
        return new ImportReportResponse(
                r.trigger(),
                r.startedAt(),
                r.finishedAt(),
                r.baskets(),
                r.depots(),
                r.syntheticDemandsCreated(),
                r.syntheticDemandCategory(),
                r.warnings(),
                snapshots.stream().map(SourceSnapshotResponse::from).toList());
    }
}
