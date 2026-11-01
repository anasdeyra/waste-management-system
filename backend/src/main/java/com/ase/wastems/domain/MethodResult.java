package com.ase.wastems.domain;

import com.ase.wastems.optimization.MethodOutcome;
import com.ase.wastems.optimization.PlanningMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One planning method's retained outcome within an evaluation run. Evaluation provenance
 * record, outside the operational class diagram (D-033).
 */
@Embeddable
public class MethodResult {

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PlanningMethod method;

    @Column(name = "feasible", nullable = false)
    private boolean feasible;

    @Column(name = "infeasibility_reason", length = 500)
    private String infeasibilityReason;

    @Column(name = "visit_order", nullable = false, length = 2000)
    private String visitOrder;

    @Column(name = "distance_m")
    private Double distanceM;

    @Column(name = "travel_time_s")
    private Double travelTimeS;

    @Column(name = "duration_s")
    private Double durationS;

    @Column(name = "execution_nanos", nullable = false)
    private long executionNanos;

    @Column(name = "coverage_ok", nullable = false)
    private boolean coverageOk;

    @Column(name = "capacity_ok", nullable = false)
    private boolean capacityOk;

    @Column(name = "algorithm_configuration", nullable = false, length = 1000)
    private String algorithmConfiguration;

    @Lob
    @Column(name = "legs_geojson")
    private String legsGeoJson;

    protected MethodResult() {}

    public static MethodResult from(MethodOutcome o, String legsGeoJson) {
        MethodResult r = new MethodResult();
        r.method = o.method();
        r.feasible = o.feasible();
        r.infeasibilityReason = o.infeasibilityReason();
        r.visitOrder = o.visitOrder().stream().map(String::valueOf).collect(Collectors.joining(","));
        r.distanceM = o.distanceM();
        r.travelTimeS = o.travelTimeS();
        r.durationS = o.durationS();
        r.executionNanos = o.executionNanos();
        r.coverageOk = o.coverageOk();
        r.capacityOk = o.capacityOk();
        r.algorithmConfiguration = o.algorithmConfiguration();
        r.legsGeoJson = legsGeoJson;
        return r;
    }

    /** Visit order as input indexes; empty when infeasible. */
    public List<Integer> visitOrderList() {
        if (visitOrder == null || visitOrder.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(visitOrder.split(",")).map(Integer::valueOf).toList();
    }

    public PlanningMethod getMethod() {
        return method;
    }

    public boolean isFeasible() {
        return feasible;
    }

    public String getInfeasibilityReason() {
        return infeasibilityReason;
    }

    public String getVisitOrder() {
        return visitOrder;
    }

    public Double getDistanceM() {
        return distanceM;
    }

    public Double getTravelTimeS() {
        return travelTimeS;
    }

    public Double getDurationS() {
        return durationS;
    }

    public long getExecutionNanos() {
        return executionNanos;
    }

    public boolean isCoverageOk() {
        return coverageOk;
    }

    public boolean isCapacityOk() {
        return capacityOk;
    }

    public String getAlgorithmConfiguration() {
        return algorithmConfiguration;
    }

    public String getLegsGeoJson() {
        return legsGeoJson;
    }
}
