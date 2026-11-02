package com.ase.wastems.service;

import com.ase.wastems.config.ExperimentProperties;
import com.ase.wastems.config.OptimizationProperties;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditOutcome;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.ScenarioKind;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.EvaluationBatchRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.routing.DirectedCostMatrixProvider;
import com.ase.wastems.routing.RoutingUnavailableException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs a seeded MN01 experiment batch (D-016, D-022, D-050): generates each scenario, evaluates it
 * with the three methods and retains one run per scenario. Deliberately not transactional: each
 * run is saved in its own transaction so that a failure never poisons the whole batch.
 */
@Service
public class ExperimentService {

    private final Mn01ScenarioPool pool;
    private final CollectionPointRepository points;
    private final DepotRepository depots;
    private final EvaluationService evaluations;
    private final EvaluationRunRepository runs;
    private final EvaluationBatchRepository batches;
    private final DirectedCostMatrixProvider matrices;
    private final ExperimentProperties experimentProps;
    private final OptimizationProperties optimizationProps;
    private final AuditService audit;
    private final TransactionTemplate tx;

    public ExperimentService(Mn01ScenarioPool pool, CollectionPointRepository points, DepotRepository depots,
            EvaluationService evaluations, EvaluationRunRepository runs, EvaluationBatchRepository batches,
            DirectedCostMatrixProvider matrices, ExperimentProperties experimentProps,
            OptimizationProperties optimizationProps, AuditService audit, PlatformTransactionManager txManager) {
        this.pool = pool;
        this.points = points;
        this.depots = depots;
        this.evaluations = evaluations;
        this.runs = runs;
        this.batches = batches;
        this.matrices = matrices;
        this.experimentProps = experimentProps;
        this.optimizationProps = optimizationProps;
        this.audit = audit;
        this.tx = new TransactionTemplate(txManager);
    }

    public EvaluationBatch run(ExperimentSpec spec, String username) {
        String version = matrices.roadGraphVersion();
        String engine = matrices.routingEngine();
        Depot depot = depots.findBySourceDatasetAndSourceIdentifier(Mn01ImportService.DEPOT_DATASET, "MN01G")
                .orElseThrow(() -> new ConflictException("The MN01G depot is missing; run the MN01 import first."));
        Map<String, UUID> pointIds = new HashMap<>();
        for (CollectionPoint p : points.findBySourceDataset(Mn01ImportService.BASKET_DATASET)) {
            pointIds.put(p.getSourceIdentifier(), p.getId());
        }
        for (PoolBasket b : pool.baskets()) {
            if (!pointIds.containsKey(b.basketId())) {
                throw new ConflictException("MN01 basket " + b.basketId() + " is missing; run the MN01 import first.");
            }
        }
        BigDecimal capacity = experimentProps.vehicleCapacityKg().setScale(2, java.math.RoundingMode.UNNECESSARY);
        List<String> snapshotIds = List.of(pool.basketSnapshotId(), pool.depotSnapshotId());
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        EvaluationBatch fresh = EvaluationBatch.startExperiment(spec.label(), spec.isCanonical(), parameters(capacity),
                spec.profiles().stream().map(Enum::name).toList(), spec.seeds(), spec.sizes(), spec.repetitions(),
                capacity, snapshotIds, version, engine, environment(), spec.scenarioCount(), spec.runCount(),
                username, now);
        UUID batchId = tx.execute(st -> batches.save(fresh)).getId();

        int done = 0;
        try {
            for (int rep = 1; rep <= spec.repetitions(); rep++) {
                for (DemandProfile profile : spec.profiles()) {
                    for (long seed : spec.seeds()) {
                        for (int size : spec.sizes()) {
                            BatchScenario scenario = scenario(profile, seed, size, rep, depot, capacity, snapshotIds,
                                    pointIds);
                            EvaluationRun run =
                                    evaluations.computeBatchRun(scenario, ScenarioKind.EXPERIMENT, batchId, username);
                            tx.executeWithoutResult(st -> {
                                runs.save(run);
                                EvaluationBatch b = batches.findById(batchId).orElseThrow();
                                b.recordRun(run.getStatus());
                                batches.save(b);
                            });
                            done++;
                        }
                    }
                }
            }
        } catch (RoutingUnavailableException e) {
            int completedRuns = done;
            tx.executeWithoutResult(st -> {
                EvaluationBatch b = batches.findById(batchId).orElseThrow();
                b.abort(e.getMessage(), Instant.now().truncatedTo(ChronoUnit.MILLIS));
                batches.save(b);
                audit.record(AuditAction.EVALUATION_EXPERIMENT_RUN, AuditEntityType.EVALUATION_BATCH, batchId,
                        AuditOutcome.FAILURE, spec.label() + ": aborted after " + completedRuns + " of "
                                + spec.runCount() + " runs: " + AuditService.brief(e.getMessage()));
            });
            throw e;
        }
        tx.executeWithoutResult(st -> {
            EvaluationBatch b = batches.findById(batchId).orElseThrow();
            b.finish(Instant.now().truncatedTo(ChronoUnit.MILLIS));
            batches.save(b);
            audit.record(AuditAction.EVALUATION_EXPERIMENT_RUN, AuditEntityType.EVALUATION_BATCH, batchId,
                    b.getFailedRunCount() > 0 ? AuditOutcome.FAILURE : AuditOutcome.SUCCESS,
                    spec.label() + ": profiles " + String.join(",", b.demandProfileList()) + "; seeds "
                            + join(b.seedList()) + "; sizes " + join(b.sizeList()) + "; repetitions "
                            + spec.repetitions() + "; runs " + b.getRunCount() + ", completed "
                            + b.getCompletedRunCount() + ", routing failed " + b.getFailedRunCount()
                            + "; canonical " + b.isCanonical());
        });
        return batches.findById(batchId).orElseThrow();
    }

    private BatchScenario scenario(DemandProfile profile, long seed, int size, int repetition, Depot depot,
            BigDecimal capacity, List<String> snapshotIds, Map<String, UUID> pointIds) {
        List<PoolBasket> sample = ScenarioGenerator.sample(pool.baskets(), seed, size);
        List<Integer> demand = ScenarioGenerator.demandsKg(profile, seed, size);
        List<FrozenStop> stops = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            PoolBasket b = sample.get(i);
            BigDecimal kg = BigDecimal.valueOf(demand.get(i)).setScale(2);
            stops.add(new FrozenStop(i + 1, pointIds.get(b.basketId()), Mn01ImportService.BASKET_DATASET,
                    b.basketId(), b.name(), b.longitude(), b.latitude(), kg, "Mixed litter=" + kg.toPlainString()));
        }
        return new BatchScenario(ScenarioGenerator.scenarioKey(profile, seed, size), profile.name(), seed, repetition,
                new FrozenDepot(depot.getName(), depot.getLongitude(), depot.getLatitude(),
                        depot.getSourceIdentifier()),
                stops, capacity, snapshotIds);
    }

    private String parameters(BigDecimal capacity) {
        return "generator " + ScenarioGenerator.VERSION + "; pool " + pool.baskets().size() + " DSNY baskets of "
                + pool.basketSnapshotId() + " sorted by basketid; locations java.util.Random(seed) Fisher-Yates "
                + "prefix; demand java.util.Random(31*seed+code) UNIFORM_LOW(1) 10-30 kg, UNIFORM_HIGH(2) 30-60 kg, "
                + "HOTSPOT(3) 1 in 5 60-120 kg else 5-20 kg; capacity " + capacity.toPlainString()
                + " kg; service time " + trimNumber(optimizationProps.serviceTimeSeconds()) + " s; depot MN01G";
    }

    private static String trimNumber(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static String join(List<?> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static String environment() {
        String text = "Java " + Runtime.version() + " (" + System.getProperty("java.vendor") + "); "
                + System.getProperty("os.name") + " " + System.getProperty("os.version") + " "
                + System.getProperty("os.arch") + "; " + Runtime.getRuntime().availableProcessors() + " CPUs";
        return text.length() > 300 ? text.substring(0, 300) : text;
    }
}
