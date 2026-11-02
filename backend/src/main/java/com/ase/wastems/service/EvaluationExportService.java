package com.ase.wastems.service;

import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.EvaluationStop;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.domain.ScenarioKind;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.repository.EvaluationBatchRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CSV exports of the evaluation data (US-38, D-028): one row per run and planning method (four methods; none for a method a run was retained without), and a
 * batch's scenarios in the import format. Operational tours, users, vehicles, agents and
 * categories are never exported.
 */
@Service
public class EvaluationExportService {

    public static final List<String> RESULT_COLUMNS = List.of("run_id", "scenario_kind", "batch_id", "scenario_key",
            "demand_profile", "seed", "repetition", "point_count", "tour_id", "tour_revision", "run_status",
            "failure_code", "created_at", "created_by", "method", "feasible", "coverage_ok", "capacity_ok",
            "infeasibility_reason", "distance_m", "travel_time_s", "service_time_per_stop_s",
            "service_time_total_s", "duration_s", "execution_time_ms", "input_order_distance_m",
            "input_order_duration_s", "distance_change_pct", "duration_change_pct", "sweep_distance_m", "sweep_duration_s",
            "distance_change_vs_sweep_pct", "duration_change_vs_sweep_pct", "total_demand_kg",
            "vehicle_capacity_kg", "visit_order", "visit_order_source_ids", "algorithm_configuration",
            "source_snapshot_ids", "road_graph_version", "routing_engine");

    public static final List<String> SCENARIO_COLUMNS = List.of("scenario_key", "input_order", "point_id",
            "source_dataset", "source_identifier", "longitude", "latitude", "category", "demand_kg",
            "vehicle_capacity_kg", "demand_profile", "seed");

    private static final int CHUNK = 20;

    private final EvaluationRunRepository runs;
    private final EvaluationBatchRepository batches;

    @PersistenceContext
    private EntityManager entityManager;

    public EvaluationExportService(EvaluationRunRepository runs, EvaluationBatchRepository batches) {
        this.runs = runs;
        this.batches = batches;
    }

    @Transactional(readOnly = true)
    public String resultsCsv(ScenarioKind kind, UUID batchId, UUID runId) {
        StringBuilder out = new StringBuilder(CsvWriter.line(RESULT_COLUMNS));
        List<UUID> ids = runs.findIdsForExport(kind, batchId, runId);
        for (int from = 0; from < ids.size(); from += CHUNK) {
            List<UUID> chunk = ids.subList(from, Math.min(ids.size(), from + CHUNK));
            Map<UUID, EvaluationRun> loaded = new HashMap<>();
            for (EvaluationRun r : runs.findAllById(chunk)) {
                loaded.put(r.getId(), r);
            }
            for (UUID id : chunk) {
                EvaluationRun r = loaded.get(id);
                if (r != null) {
                    appendRows(out, r);
                }
            }
            entityManager.clear();
        }
        return out.toString();
    }

    private static void appendRows(StringBuilder out, EvaluationRun r) {
        MethodResult io = r.getResults().stream().filter(m -> m.getMethod() == PlanningMethod.INPUT_ORDER)
                .findFirst().orElse(null);
        MethodResult sw = r.getResults().stream().filter(m -> m.getMethod() == PlanningMethod.SWEEP).findFirst()
                .orElse(null);
        boolean swOk = sw != null && sw.isFeasible();
        boolean completed = r.getStatus() == EvaluationStatus.COMPLETED;
        for (PlanningMethod method : PlanningMethod.values()) {
            MethodResult m = r.getResults().stream().filter(x -> x.getMethod() == method).findFirst().orElse(null);
            if (completed && m == null) {
                continue; // a run retained before the method existed (D-052) has no row for it
            }
            List<String> f = new ArrayList<>();
            f.add(r.getId().toString());
            f.add(r.getScenarioKind().name());
            f.add(text(r.getBatchId()));
            f.add(r.getScenarioKey());
            f.add(r.getDemandProfile());
            f.add(text(r.getSeed()));
            f.add(text(r.getRepetition()));
            f.add(String.valueOf(r.getPointCount()));
            f.add(text(r.getTourId()));
            f.add(text(r.getTourRevision()));
            f.add(r.getStatus().name());
            f.add(r.getFailureCode());
            f.add(iso(r.getCreatedAt()));
            f.add(r.getCreatedBy());
            f.add(method.name());
            if (!completed || m == null) {
                f.add("false");
                f.add("");
                f.add("");
                f.add(r.getFailureMessage());
                for (int i = 0; i < 14; i++) {
                    f.add("");
                }
            } else if (!m.isFeasible()) {
                f.add("false");
                f.add(String.valueOf(m.isCoverageOk()));
                f.add(String.valueOf(m.isCapacityOk()));
                f.add(m.getInfeasibilityReason());
                for (int i = 0; i < 5; i++) {
                    f.add("");
                }
                f.add(num(m.getExecutionNanos() / 1e6, "%.6f"));
                for (int i = 0; i < 8; i++) {
                    f.add("");
                }
            } else {
                f.add("true");
                f.add(String.valueOf(m.isCoverageOk()));
                f.add(String.valueOf(m.isCapacityOk()));
                f.add("");
                f.add(num(m.getDistanceM(), "%.3f"));
                f.add(num(m.getTravelTimeS(), "%.3f"));
                f.add(num(r.getServiceTimeSeconds(), "%.3f"));
                f.add(num(r.getServiceTimeSeconds() * r.getPointCount(), "%.3f"));
                f.add(num(m.getDurationS(), "%.3f"));
                f.add(num(m.getExecutionNanos() / 1e6, "%.6f"));
                boolean ioOk = io != null && io.isFeasible();
                f.add(ioOk ? num(io.getDistanceM(), "%.3f") : "");
                f.add(ioOk ? num(io.getDurationS(), "%.3f") : "");
                f.add(num(EvaluationComparison.changePct(true, true, m.getDistanceM(), ioOk,
                        io == null ? null : io.getDistanceM()), "%.4f"));
                f.add(num(EvaluationComparison.changePct(true, true, m.getDurationS(), ioOk,
                        io == null ? null : io.getDurationS()), "%.4f"));
                f.add(swOk ? num(sw.getDistanceM(), "%.3f") : "");
                f.add(swOk ? num(sw.getDurationS(), "%.3f") : "");
                boolean vs = EvaluationComparison.comparedWithSweep(method);
                f.add(vs ? num(EvaluationComparison.changePct(true, true, m.getDistanceM(), swOk,
                        sw == null ? null : sw.getDistanceM()), "%.4f") : "");
                f.add(vs ? num(EvaluationComparison.changePct(true, true, m.getDurationS(), swOk,
                        sw == null ? null : sw.getDurationS()), "%.4f") : "");
            }
            f.add(r.getTotalDemandKg().toPlainString());
            f.add(r.getVehicleCapacityKg().toPlainString());
            if (completed && m != null && m.isFeasible()) {
                List<Integer> order = m.visitOrderList();
                List<String> parts = new ArrayList<>();
                List<String> ids = new ArrayList<>();
                for (int idx : order) {
                    parts.add(String.valueOf(idx));
                    EvaluationStop s = r.getStops().get(idx - 1);
                    ids.add(s.getSourceIdentifier() != null && !s.getSourceIdentifier().isEmpty()
                            ? s.getSourceIdentifier()
                            : "point:" + s.getCollectionPointId());
                }
                f.add(String.join(" ", parts));
                f.add(String.join(" ", ids));
                f.add(m.getAlgorithmConfiguration());
            } else if (completed && m != null) {
                f.add("");
                f.add("");
                f.add(m.getAlgorithmConfiguration());
            } else {
                f.add("");
                f.add("");
                f.add("");
            }
            f.add(r.getSourceSnapshotIds());
            f.add(r.getRoadGraphVersion());
            f.add(r.getRoutingEngine());
            out.append(CsvWriter.line(f));
        }
    }

    @Transactional(readOnly = true)
    public String scenariosCsv(UUID batchId) {
        if (!batches.existsById(batchId)) {
            throw new NotFoundException("Evaluation batch not found: " + batchId);
        }
        StringBuilder out = new StringBuilder(CsvWriter.line(SCENARIO_COLUMNS));
        for (EvaluationRun r : runs.findByBatchIdOrderByCreatedAtAscIdAsc(batchId)) {
            if (r.getRepetition() != null && r.getRepetition() != 1) {
                continue;
            }
            for (EvaluationStop s : r.getStops()) {
                boolean hasSource = s.getSourceDataset() != null && !s.getSourceDataset().isEmpty()
                        && s.getSourceIdentifier() != null && !s.getSourceIdentifier().isEmpty();
                for (String[] cat : categories(s)) {
                    List<String> f = new ArrayList<>();
                    f.add(r.getScenarioKey());
                    f.add(String.valueOf(s.getInputIndex()));
                    f.add(hasSource ? "" : s.getCollectionPointId().toString());
                    f.add(hasSource ? s.getSourceDataset() : "");
                    f.add(hasSource ? s.getSourceIdentifier() : "");
                    f.add(Double.toString(s.getLongitude()));
                    f.add(Double.toString(s.getLatitude()));
                    f.add(cat[0]);
                    f.add(cat[1]);
                    f.add(r.getVehicleCapacityKg().toPlainString());
                    f.add(r.getDemandProfile());
                    f.add(text(r.getSeed()));
                    out.append(CsvWriter.line(f));
                }
            }
        }
        entityManager.clear();
        return out.toString();
    }

    /** Category and kilograms (two decimals) of a stop; a stop without breakdown is Mixed litter. */
    private static List<String[]> categories(EvaluationStop s) {
        List<String[]> list = new ArrayList<>();
        String b = s.getDemandBreakdown();
        if (b == null || b.isBlank()) {
            list.add(new String[] {"Mixed litter", s.getDemandKg().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()});
            return list;
        }
        for (String part : b.split("; ")) {
            int eq = part.lastIndexOf('=');
            if (eq < 0) {
                continue;
            }
            list.add(new String[] {part.substring(0, eq),
                    new BigDecimal(part.substring(eq + 1)).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()});
        }
        return list;
    }

    private static String text(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String iso(Instant t) {
        return t == null ? "" : t.toString();
    }

    private static String num(Double v, String format) {
        return v == null ? "" : String.format(Locale.ROOT, format, v);
    }
}
