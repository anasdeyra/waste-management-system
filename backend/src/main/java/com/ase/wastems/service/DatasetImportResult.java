package com.ase.wastems.service;

/**
 * Outcome of importing one source file. {@code created + existing + skipped == rowCount};
 * {@code updated} is the subset of {@code existing} whose source label changed.
 * {@code dsnyEligibleRowCount} is null for datasets without basket types.
 */
public record DatasetImportResult(
        String sourceDataset,
        String sourceFile,
        int rowCount,
        Integer dsnyEligibleRowCount,
        int created,
        int updated,
        int existing,
        int skipped,
        String sha256,
        String snapshotId) {}
