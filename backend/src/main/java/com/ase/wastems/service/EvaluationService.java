package com.ase.wastems.service;

import com.ase.wastems.config.OptimizationProperties;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStop;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.optimization.MethodOutcome;
import com.ase.wastems.optimization.PlanningScenario;
import com.ase.wastems.optimization.TourOptimizationModule;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.routing.DirectedCostMatrixProvider;
import com.ase.wastems.routing.RoadLegUnavailableException;
import com.ase.wastems.routing.RoadMatrix;
import com.ase.wastems.routing.WgsPoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the three methods on a frozen scenario and retains the run (US-21, US-22). Routing and
 * optimization run outside any transaction; only the final save is transactional. The tour is
 * never touched.
 */
@Service
public class EvaluationService {

    private final DirectedCostMatrixProvider matrices;
    private final TourOptimizationModule optimizer;
    private final EvaluationRunRepository runs;
    private final OptimizationProperties props;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate txTemplate;

    public EvaluationService(DirectedCostMatrixProvider matrices, TourOptimizationModule optimizer,
            EvaluationRunRepository runs, OptimizationProperties props, ObjectMapper objectMapper,
            PlatformTransactionManager txManager) {
        this.matrices = matrices;
        this.optimizer = optimizer;
        this.runs = runs;
        this.props = props;
        this.objectMapper = objectMapper;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /** RoutingUnavailableException propagates and nothing is retained. */
    public EvaluationRun evaluate(FrozenScenario s, String username) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        List<WgsPoint> nodes = new ArrayList<>();
        nodes.add(new WgsPoint(s.depot().longitude(), s.depot().latitude()));
        List<EvaluationStop> frozenStops = new ArrayList<>();
        for (FrozenStop st : s.stops()) {
            nodes.add(new WgsPoint(st.longitude(), st.latitude()));
            frozenStops.add(new EvaluationStop(st.inputIndex(), st.collectionPointId(), st.sourceDataset(),
                    st.sourceIdentifier(), st.name(), st.longitude(), st.latitude(), st.demandKg()));
        }
        EvaluationRun run;
        try {
            RoadMatrix m = matrices.buildMatrix(nodes);
            run = newRun(s, username, now, m.roadGraphVersion(), frozenStops);
            double[] demand = new double[s.stops().size()];
            for (int i = 0; i < demand.length; i++) {
                demand[i] = s.stops().get(i).demandKg().doubleValue();
            }
            PlanningScenario ps = new PlanningScenario(m, demand, s.vehicleCapacityKg().doubleValue(),
                    props.serviceTimeSeconds());
            List<MethodResult> results = new ArrayList<>();
            for (MethodOutcome o : optimizer.planAll(ps)) {
                results.add(MethodResult.from(o, o.feasible() ? legsGeoJson(m, o.visitOrder()) : null));
            }
            run.complete(matrixJson(m), results);
        } catch (RoadLegUnavailableException e) {
            String version = matrices.roadGraphVersion();
            run = newRun(s, username, now, version, frozenStops);
            run.failRouting(e.getReason().name(), describe(e, s, version));
        }
        EvaluationRun toSave = run;
        return txTemplate.execute(st -> runs.save(toSave));
    }

    private EvaluationRun newRun(FrozenScenario s, String username, Instant now, String version,
            List<EvaluationStop> frozenStops) {
        return EvaluationRun.forTourDraft(s.tourId(), s.tourRevision(), username, now, version,
                matrices.routingEngine(), s.sourceSnapshotIds(), s.depot().name(), s.depot().longitude(),
                s.depot().latitude(), s.depot().sourceIdentifier(), s.vehicleCapacityKg(),
                props.serviceTimeSeconds(), frozenStops);
    }

    private static String label(FrozenScenario s, int node) {
        if (node <= 0) {
            return "the depot (" + s.depot().name() + ")";
        }
        return "stop " + node + " (" + s.stops().get(node - 1).name() + ")";
    }

    private static String describe(RoadLegUnavailableException e, FrozenScenario s, String version) {
        return switch (e.getReason()) {
            case POINT_NOT_ROUTABLE -> label(s, e.getFromNode()) + " cannot be routed on road graph " + version
                    + ": " + e.getMessage();
            case NO_ROAD_PATH -> "No directed road path from " + label(s, e.getFromNode()) + " to "
                    + label(s, e.getToNode()) + " on road graph " + version + ": " + e.getMessage();
        };
    }

    private String matrixJson(RoadMatrix m) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("nodeCount", m.size());
        doc.put("distanceM", m.distanceMatrix());
        doc.put("travelTimeS", m.travelTimeMatrix());
        return toJson(doc);
    }

    private String legsGeoJson(RoadMatrix m, List<Integer> order) {
        List<Integer> route = new ArrayList<>();
        route.add(0);
        route.addAll(order);
        route.add(0);
        List<Object> features = new ArrayList<>();
        for (int k = 0; k + 1 < route.size(); k++) {
            int from = route.get(k);
            int to = route.get(k + 1);
            List<List<Double>> coords = new ArrayList<>();
            for (WgsPoint p : m.leg(from, to).geometry()) {
                coords.add(List.of(p.longitude(), p.latitude()));
            }
            if (coords.size() == 1) {
                coords.add(coords.get(0));
            }
            Map<String, Object> geometry = new LinkedHashMap<>();
            geometry.put("type", "LineString");
            geometry.put("coordinates", coords);
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("sequence", k + 1);
            properties.put("fromIndex", from);
            properties.put("toIndex", to);
            properties.put("distanceM", m.distance(from, to));
            properties.put("travelTimeS", m.travelTime(from, to));
            Map<String, Object> feature = new LinkedHashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", geometry);
            feature.put("properties", properties);
            features.add(feature);
        }
        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("type", "FeatureCollection");
        collection.put("features", features);
        return toJson(collection);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize evaluation data", e);
        }
    }

    @Transactional(readOnly = true)
    public EvaluationRun get(UUID runId) {
        EvaluationRun run = runs.findById(runId)
                .orElseThrow(() -> new NotFoundException("Evaluation run not found: " + runId));
        return initialized(run);
    }

    @Transactional(readOnly = true)
    public List<EvaluationRun> listForTour(UUID tourId) {
        List<EvaluationRun> list = runs.findByTourIdOrderByCreatedAtDesc(tourId);
        list.forEach(EvaluationService::initialized);
        return list;
    }

    private static EvaluationRun initialized(EvaluationRun run) {
        run.getStops().size();
        run.getResults().size();
        return run;
    }
}
