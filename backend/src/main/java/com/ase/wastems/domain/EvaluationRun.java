package com.ase.wastems.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Retained three-method evaluation of one frozen scenario: inputs (stops, depot, capacity,
 * service time, snapshot and road graph identifiers) and, when routing succeeded, the directed
 * matrix and each method's result. Evaluation provenance record, outside the operational class
 * diagram (D-033). The tour id is a plain column without a foreign key: runs are history.
 */
@Entity
@Table(name = "evaluation_run")
public class EvaluationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "scenario_kind", nullable = false, length = 20)
    private ScenarioKind scenarioKind;

    @Column(name = "tour_id")
    private UUID tourId;

    @Column(name = "tour_revision")
    private Long tourRevision;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "scenario_key", length = 80)
    private String scenarioKey;

    @Column(name = "repetition")
    private Integer repetition;

    @Column(name = "point_count")
    private Integer pointCount;

    @Column(name = "demand_profile", length = 40)
    private String demandProfile;

    @Column(name = "seed")
    private Long seed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EvaluationStatus status;

    @Column(name = "failure_code", length = 40)
    private String failureCode;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "road_graph_version", nullable = false, length = 80)
    private String roadGraphVersion;

    @Column(name = "routing_engine", nullable = false, length = 200)
    private String routingEngine;

    @Column(name = "source_snapshot_ids", nullable = false, length = 1000)
    private String sourceSnapshotIds;

    @Column(name = "depot_name", nullable = false, length = 120)
    private String depotName;

    @Column(name = "depot_longitude", nullable = false)
    private double depotLongitude;

    @Column(name = "depot_latitude", nullable = false)
    private double depotLatitude;

    @Column(name = "depot_source_identifier", length = 40)
    private String depotSourceIdentifier;

    @Column(name = "vehicle_capacity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal vehicleCapacityKg;

    @Column(name = "service_time_seconds", nullable = false)
    private double serviceTimeSeconds;

    @Column(name = "total_demand_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDemandKg;

    @Lob
    @Column(name = "matrix_json")
    private String matrixJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 20)
    private ProposalDecision decision;

    @Column(name = "decided_by", length = 50)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @ElementCollection
    @CollectionTable(name = "evaluation_stop", joinColumns = @JoinColumn(name = "run_id"))
    @OrderColumn(name = "stop_position")
    private List<EvaluationStop> stops = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "evaluation_method_result", joinColumns = @JoinColumn(name = "run_id"))
    @OrderColumn(name = "result_position")
    private List<MethodResult> results = new ArrayList<>();

    protected EvaluationRun() {}

    public static EvaluationRun forTourDraft(UUID tourId, long tourRevision, String createdBy, Instant createdAt,
            String roadGraphVersion, String routingEngine, List<String> sourceSnapshotIds,
            String depotName, double depotLongitude, double depotLatitude, String depotSourceIdentifier,
            BigDecimal vehicleCapacityKg, double serviceTimeSeconds, List<EvaluationStop> stops) {
        EvaluationRun r = new EvaluationRun();
        r.scenarioKind = ScenarioKind.TOUR_DRAFT;
        r.tourId = tourId;
        r.tourRevision = tourRevision;
        r.initialize(createdBy, createdAt, roadGraphVersion, routingEngine, sourceSnapshotIds, depotName,
                depotLongitude, depotLatitude, depotSourceIdentifier, vehicleCapacityKg, serviceTimeSeconds, stops);
        return r;
    }

    /** Run of a seeded experiment scenario or a CSV-imported scenario (kind EXPERIMENT or CSV_IMPORT). */
    public static EvaluationRun forBatchScenario(ScenarioKind kind, UUID batchId, String scenarioKey,
            String demandProfile, Long seed, Integer repetition, String createdBy, Instant createdAt,
            String roadGraphVersion, String routingEngine, List<String> sourceSnapshotIds, String depotName,
            double depotLongitude, double depotLatitude, String depotSourceIdentifier,
            BigDecimal vehicleCapacityKg, double serviceTimeSeconds, List<EvaluationStop> stops) {
        if (kind != ScenarioKind.EXPERIMENT && kind != ScenarioKind.CSV_IMPORT) {
            throw new IllegalArgumentException("Batch scenarios are EXPERIMENT or CSV_IMPORT, not " + kind);
        }
        EvaluationRun r = new EvaluationRun();
        r.scenarioKind = kind;
        r.batchId = batchId;
        r.scenarioKey = scenarioKey;
        r.demandProfile = demandProfile;
        r.seed = seed;
        r.repetition = repetition;
        r.initialize(createdBy, createdAt, roadGraphVersion, routingEngine, sourceSnapshotIds, depotName,
                depotLongitude, depotLatitude, depotSourceIdentifier, vehicleCapacityKg, serviceTimeSeconds, stops);
        return r;
    }

    private void initialize(String createdBy, Instant createdAt, String roadGraphVersion, String routingEngine,
            List<String> sourceSnapshotIds, String depotName, double depotLongitude, double depotLatitude,
            String depotSourceIdentifier, BigDecimal vehicleCapacityKg, double serviceTimeSeconds,
            List<EvaluationStop> stops) {
        this.status = EvaluationStatus.COMPLETED;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.roadGraphVersion = roadGraphVersion;
        this.routingEngine = routingEngine;
        this.sourceSnapshotIds = String.join(";", sourceSnapshotIds);
        this.depotName = depotName;
        this.depotLongitude = depotLongitude;
        this.depotLatitude = depotLatitude;
        this.depotSourceIdentifier = depotSourceIdentifier;
        this.vehicleCapacityKg = vehicleCapacityKg;
        this.serviceTimeSeconds = serviceTimeSeconds;
        this.stops = new ArrayList<>(stops);
        this.pointCount = stops.size();
        BigDecimal total = BigDecimal.ZERO;
        for (EvaluationStop s : stops) {
            total = total.add(s.getDemandKg());
        }
        this.totalDemandKg = total;
    }

    public void complete(String matrixJson, List<MethodResult> results) {
        this.status = EvaluationStatus.COMPLETED;
        this.matrixJson = matrixJson;
        this.results = new ArrayList<>(results);
    }

    public void failRouting(String failureCode, String failureMessage) {
        this.status = EvaluationStatus.ROUTING_FAILED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage != null && failureMessage.length() > 1000
                ? failureMessage.substring(0, 1000)
                : failureMessage;
        this.results = new ArrayList<>();
        this.matrixJson = null;
    }

    public void recordDecision(ProposalDecision decision, String decidedBy, Instant decidedAt) {
        this.decision = decision;
        this.decidedBy = decidedBy;
        this.decidedAt = decidedAt;
    }

    public ProposalDecision getDecision() {
        return decision;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public List<String> sourceSnapshotIdList() {
        if (sourceSnapshotIds == null || sourceSnapshotIds.isEmpty()) {
            return List.of();
        }
        return List.of(sourceSnapshotIds.split(";"));
    }

    public int getPointCount() {
        return stops.size();
    }

    public UUID getId() {
        return id;
    }

    public ScenarioKind getScenarioKind() {
        return scenarioKind;
    }

    public UUID getTourId() {
        return tourId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public String getScenarioKey() {
        return scenarioKey;
    }

    public Integer getRepetition() {
        return repetition;
    }

    public Long getTourRevision() {
        return tourRevision;
    }

    public String getDemandProfile() {
        return demandProfile;
    }

    public Long getSeed() {
        return seed;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getRoadGraphVersion() {
        return roadGraphVersion;
    }

    public String getRoutingEngine() {
        return routingEngine;
    }

    public String getSourceSnapshotIds() {
        return sourceSnapshotIds;
    }

    public String getDepotName() {
        return depotName;
    }

    public double getDepotLongitude() {
        return depotLongitude;
    }

    public double getDepotLatitude() {
        return depotLatitude;
    }

    public String getDepotSourceIdentifier() {
        return depotSourceIdentifier;
    }

    public BigDecimal getVehicleCapacityKg() {
        return vehicleCapacityKg;
    }

    public double getServiceTimeSeconds() {
        return serviceTimeSeconds;
    }

    public BigDecimal getTotalDemandKg() {
        return totalDemandKg;
    }

    public String getMatrixJson() {
        return matrixJson;
    }

    public List<EvaluationStop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public List<MethodResult> getResults() {
        return Collections.unmodifiableList(results);
    }
}
