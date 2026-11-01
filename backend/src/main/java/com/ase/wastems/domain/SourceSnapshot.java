package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Provenance record of an imported source file: dataset, file, row count, checksum and the
 * derived snapshot identifier. Outside the operational class diagram (D-033).
 */
@Entity
@Table(
        name = "source_snapshot",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_snapshot_dataset", columnNames = "source_dataset"))
public class SourceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_dataset", nullable = false, length = 80)
    private String sourceDataset;

    @Column(name = "source_file", nullable = false, length = 200)
    private String sourceFile;

    @Column(name = "row_count")
    private int rowCount;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "snapshot_id", nullable = false, length = 120)
    private String snapshotId;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected SourceSnapshot() {}

    public SourceSnapshot(
            String sourceDataset,
            String sourceFile,
            int rowCount,
            String sha256,
            String snapshotId,
            Instant importedAt) {
        this.sourceDataset = sourceDataset;
        this.sourceFile = sourceFile;
        this.rowCount = rowCount;
        this.sha256 = sha256;
        this.snapshotId = snapshotId;
        this.importedAt = importedAt;
    }

    public void update(
            String sourceFile, int rowCount, String sha256, String snapshotId, Instant importedAt) {
        this.sourceFile = sourceFile;
        this.rowCount = rowCount;
        this.sha256 = sha256;
        this.snapshotId = snapshotId;
        this.importedAt = importedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSourceDataset() {
        return sourceDataset;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public int getRowCount() {
        return rowCount;
    }

    public String getSha256() {
        return sha256;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public Instant getImportedAt() {
        return importedAt;
    }
}
