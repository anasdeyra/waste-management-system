package com.ase.wastems.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One experiment execution or one CSV import: the provenance of a set of evaluation runs.
 * Evaluation provenance record, outside the operational class diagram (D-033). Runs refer to it
 * by a plain batch id column.
 */
@Entity
@Table(name = "evaluation_batch")
public class EvaluationBatch {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ScenarioKind kind;

    @Column(nullable = false, length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private BatchStatus status;

    @Column(nullable = false)
    private boolean canonical;

    @Column(length = 1000)
    private String parameters;

    @Column(name = "demand_profiles", length = 200)
    private String demandProfiles;

    @Column(length = 200)
    private String seeds;

    @Column(length = 200)
    private String sizes;

    @Column(name = "repetitions")
    private Integer repetitions;

    @Column(name = "vehicle_capacity_kg", precision = 10, scale = 2)
    private BigDecimal vehicleCapacityKg;

    @Column(name = "source_snapshot_ids", length = 1000)
    private String sourceSnapshotIds;

    @Column(name = "road_graph_version", length = 80)
    private String roadGraphVersion;

    @Column(name = "routing_engine", length = 200)
    private String routingEngine;

    @Column(length = 300)
    private String environment;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "file_sha256", length = 64)
    private String fileSha256;

    @Column(name = "file_row_count")
    private Integer fileRowCount;

    @Column(name = "scenario_count", nullable = false)
    private int scenarioCount;

    @Column(name = "run_count", nullable = false)
    private int runCount;

    @Column(name = "completed_run_count", nullable = false)
    private int completedRunCount;

    @Column(name = "failed_run_count", nullable = false)
    private int failedRunCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    protected EvaluationBatch() {}

    public static EvaluationBatch startExperiment(String label, boolean canonical, String parameters,
            List<String> demandProfiles, List<Long> seeds, List<Integer> sizes, int repetitions,
            BigDecimal vehicleCapacityKg, List<String> sourceSnapshotIds, String roadGraphVersion,
            String routingEngine, String environment, int scenarioCount, int runCount, String createdBy,
            Instant createdAt) {
        EvaluationBatch b = new EvaluationBatch();
        b.id = UUID.randomUUID();
        b.kind = ScenarioKind.EXPERIMENT;
        b.label = label;
        b.status = BatchStatus.RUNNING;
        b.canonical = canonical;
        b.parameters = parameters;
        b.demandProfiles = join(demandProfiles, ",");
        b.seeds = join(seeds, ",");
        b.sizes = join(sizes, ",");
        b.repetitions = repetitions;
        b.vehicleCapacityKg = vehicleCapacityKg;
        b.sourceSnapshotIds = join(sourceSnapshotIds, ";");
        b.roadGraphVersion = roadGraphVersion;
        b.routingEngine = routingEngine;
        b.environment = environment;
        b.scenarioCount = scenarioCount;
        b.runCount = runCount;
        b.createdBy = createdBy;
        b.createdAt = createdAt;
        return b;
    }

    public static EvaluationBatch startCsvImport(String label, String fileName, String fileSha256,
            int fileRowCount, List<String> sourceSnapshotIds, String roadGraphVersion, String routingEngine,
            String environment, int scenarioCount, String createdBy, Instant createdAt) {
        EvaluationBatch b = new EvaluationBatch();
        b.id = UUID.randomUUID();
        b.kind = ScenarioKind.CSV_IMPORT;
        b.label = label;
        b.status = BatchStatus.RUNNING;
        b.canonical = false;
        b.fileName = fileName;
        b.fileSha256 = fileSha256;
        b.fileRowCount = fileRowCount;
        b.sourceSnapshotIds = join(sourceSnapshotIds, ";");
        b.roadGraphVersion = roadGraphVersion;
        b.routingEngine = routingEngine;
        b.environment = environment;
        b.scenarioCount = scenarioCount;
        b.runCount = scenarioCount;
        b.createdBy = createdBy;
        b.createdAt = createdAt;
        return b;
    }

    private static String join(List<?> values, String separator) {
        if (values == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (Object v : values) {
            parts.add(String.valueOf(v));
        }
        return String.join(separator, parts);
    }

    private static List<String> split(String text, String separator) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return List.of(text.split(separator));
    }

    public void recordRun(EvaluationStatus runStatus) {
        if (runStatus == EvaluationStatus.ROUTING_FAILED) {
            failedRunCount++;
        } else {
            completedRunCount++;
        }
    }

    public void finish(Instant at) {
        this.status = BatchStatus.COMPLETED;
        this.finishedAt = at;
    }

    public void abort(String message, Instant at) {
        this.status = BatchStatus.ABORTED;
        this.finishedAt = at;
        this.failureMessage = message != null && message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    public List<String> demandProfileList() {
        return split(demandProfiles, ",");
    }

    public List<Long> seedList() {
        return split(seeds, ",").stream().map(Long::valueOf).toList();
    }

    public List<Integer> sizeList() {
        return split(sizes, ",").stream().map(Integer::valueOf).toList();
    }

    public List<String> sourceSnapshotIdList() {
        return split(sourceSnapshotIds, ";");
    }

    public UUID getId() {
        return id;
    }

    public ScenarioKind getKind() {
        return kind;
    }

    public String getLabel() {
        return label;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public boolean isCanonical() {
        return canonical;
    }

    public String getParameters() {
        return parameters;
    }

    public Integer getRepetitions() {
        return repetitions;
    }

    public BigDecimal getVehicleCapacityKg() {
        return vehicleCapacityKg;
    }

    public String getRoadGraphVersion() {
        return roadGraphVersion;
    }

    public String getRoutingEngine() {
        return routingEngine;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileSha256() {
        return fileSha256;
    }

    public Integer getFileRowCount() {
        return fileRowCount;
    }

    public int getScenarioCount() {
        return scenarioCount;
    }

    public int getRunCount() {
        return runCount;
    }

    public int getCompletedRunCount() {
        return completedRunCount;
    }

    public int getFailedRunCount() {
        return failedRunCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getFailureMessage() {
        return failureMessage;
    }
}
