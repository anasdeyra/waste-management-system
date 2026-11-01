package com.ase.wastems.web.dto;

import com.ase.wastems.domain.SourceSnapshot;
import java.time.Instant;

/** Provenance of an imported source file. */
public record SourceSnapshotResponse(
        String sourceDataset,
        String sourceFile,
        int rowCount,
        String sha256,
        String snapshotId,
        Instant importedAt) {

    public static SourceSnapshotResponse from(SourceSnapshot s) {
        return new SourceSnapshotResponse(
                s.getSourceDataset(),
                s.getSourceFile(),
                s.getRowCount(),
                s.getSha256(),
                s.getSnapshotId(),
                s.getImportedAt());
    }
}
