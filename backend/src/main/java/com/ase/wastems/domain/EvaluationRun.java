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
    @Column(name = "scenario_kind", nullable = false, length = 20)
    private ScenarioKind scenarioKind;

    @Column(name = "tour_id")
    private UUID tourId;

    @Column(name = "tour_revision")
    private Long tourRevision;

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
        r.status = EvaluationStatus.COMPLETED;
        r.createdAt = createdAt;
        r.createdBy = createdBy;
        r.roadGraphVersion = roadGraphVersion;
        r.routingEngine = routingEngine;
        r.sourceSnapshotIds = String.join(";", sourceSnapshotIds);
        r.depotName = depotName;
        r.depotLongitude = depotLongitude;
        r.depotLatitude = depotLatitude;
        r.depotSourceIdentifier = depotSourceIdentifier;
        r.vehicleCapacityKg = vehicleCapacityKg;
        r.serviceTimeSeconds = serviceTimeSeconds;
        r.stops = new ArrayList<>(stops);
        BigDecimal total = BigDecimal.ZERO;
        for (EvaluationStop s : stops) {
            total = total.add(s.getDemandKg());
        }
        r.totalDemandKg = total;
        return r;
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
