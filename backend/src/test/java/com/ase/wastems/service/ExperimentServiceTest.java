package com.ase.wastems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ase.wastems.domain.BatchStatus;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStop;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.domain.ScenarioKind;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.TourRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class ExperimentServiceTest {

    static final String GOLDEN = "generator mn01-v1; pool 238 DSNY baskets of 8znf-7b2c:449:a0737237e370 sorted by basketid";

    @Autowired ExperimentService experiments;
    @Autowired EvaluationRunRepository evaluationRuns;
    @Autowired CollectionPointRepository points;
    @Autowired DepotRepository depots;
    @Autowired TourRepository tours;
    @Autowired SourceSnapshotRepository snapshots;
    @Autowired Mn01ImportService mn01Import;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        clean();
        mn01Import.importAll(ImportTrigger.MANUAL);
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    private void clean() {
        jdbc.update("delete from evaluation_batch");
        evaluationRuns.deleteAll();
        jdbc.update("delete from audit_event");
        tours.deleteAll();
        points.deleteAll();
        depots.deleteAll();
        snapshots.deleteAll();
    }

    private List<EvaluationRun> runsOf(EvaluationBatch b) {
        List<EvaluationRun> list = evaluationRuns.findAll().stream()
                .filter(r -> b.getId().equals(r.getBatchId())).toList();
        return list;
    }

    private static ExperimentSpec spec(List<DemandProfile> p, List<Long> s, List<Integer> z) {
        return ExperimentSpec.of(p, s, z, null, null);
    }

    @Test
    @Transactional
    void retainsOneRunPerScenarioWithProvenance() {
        EvaluationBatch b = experiments.run(
                spec(List.of(DemandProfile.UNIFORM_LOW, DemandProfile.HOTSPOT), List.of(3L), List.of(6, 12)), "admin1");
        assertThat(b.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(b.isCanonical()).isFalse();
        assertThat(b.getScenarioCount()).isEqualTo(4);
        assertThat(b.getRunCount()).isEqualTo(4);
        assertThat(b.getCompletedRunCount()).isEqualTo(4);
        assertThat(b.getFailedRunCount()).isZero();
        assertThat(b.getLabel()).isEqualTo("MN01 experiment subset");
        assertThat(b.getParameters()).startsWith(GOLDEN);
        List<EvaluationRun> runs = runsOf(b);
        assertThat(runs).hasSize(4);
        Map<String, EvaluationRun> byKey = runs.stream().collect(Collectors.toMap(EvaluationRun::getScenarioKey, r -> r));
        assertThat(byKey.keySet()).containsExactlyInAnyOrder("mn01-UNIFORM_LOW-s3-n6", "mn01-UNIFORM_LOW-s3-n12",
                "mn01-HOTSPOT-s3-n6", "mn01-HOTSPOT-s3-n12");
        Map<String, UUID> pointIds = points.findBySourceDataset("8znf-7b2c").stream()
                .collect(Collectors.toMap(CollectionPoint::getSourceIdentifier, CollectionPoint::getId));
        for (EvaluationRun r : runs) {
            assertThat(r.getScenarioKind()).isEqualTo(ScenarioKind.EXPERIMENT);
            assertThat(r.getTourId()).isNull();
            assertThat(r.getDemandProfile()).isNotNull();
            assertThat(r.getSeed()).isEqualTo(3L);
            assertThat(r.getRepetition()).isEqualTo(1);
            assertThat(r.getPointCount()).isEqualTo(r.getStops().size());
            assertThat(r.getResults()).extracting(MethodResult::getMethod).containsExactly(PlanningMethod.INPUT_ORDER,
                    PlanningMethod.NEAREST_NEIGHBOR, PlanningMethod.NN_PLUS_2_OPT);
            assertThat(r.sourceSnapshotIdList())
                    .containsExactly("8znf-7b2c:449:a0737237e370", "xw3j-2yxf:1:207851c2e229");
            assertThat(r.getRoadGraphVersion()).isEqualTo("osm-mn01-e1f2fabaa937");
            assertThat(r.getVehicleCapacityKg()).isEqualByComparingTo("10000.00");
            assertThat(r.getDepotSourceIdentifier()).isEqualTo("MN01G");
            for (EvaluationStop s : r.getStops()) {
                assertThat(s.getCollectionPointId()).isEqualTo(pointIds.get(s.getSourceIdentifier()));
                assertThat(s.getDemandBreakdown()).matches("Mixed litter=\\d+\\.00");
            }
        }
        List<Map<String, Object>> audit = jdbc.queryForList(
                "select action, entity_type, entity_id, outcome, details from audit_event where action = 'EVALUATION_EXPERIMENT_RUN'");
        assertThat(audit).hasSize(1);
        assertThat(audit.get(0).get("ENTITY_TYPE")).isEqualTo("EVALUATION_BATCH");
        assertThat(audit.get(0).get("ENTITY_ID")).isEqualTo(b.getId().toString());
        assertThat(audit.get(0).get("OUTCOME")).isEqualTo("SUCCESS");
        assertThat((String) audit.get(0).get("DETAILS")).endsWith("canonical false");
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action = 'TOUR_EVALUATED'", Long.class))
                .isZero();
    }

    @Test
    @Transactional
    void sameSpecTwiceIsDeterministic() {
        ExperimentSpec spec = spec(List.of(DemandProfile.UNIFORM_LOW, DemandProfile.HOTSPOT), List.of(3L), List.of(6, 12));
        EvaluationBatch b1 = experiments.run(spec, "admin1");
        EvaluationBatch b2 = experiments.run(spec, "admin1");
        Map<String, EvaluationRun> r1 = runsOf(b1).stream().collect(Collectors.toMap(EvaluationRun::getScenarioKey, r -> r));
        Map<String, EvaluationRun> r2 = runsOf(b2).stream().collect(Collectors.toMap(EvaluationRun::getScenarioKey, r -> r));
        assertThat(r1.keySet()).isEqualTo(r2.keySet());
        for (String key : r1.keySet()) {
            EvaluationRun a = r1.get(key);
            EvaluationRun b = r2.get(key);
            assertThat(a.getStops()).hasSameSizeAs(b.getStops());
            for (int i = 0; i < a.getStops().size(); i++) {
                EvaluationStop x = a.getStops().get(i);
                EvaluationStop y = b.getStops().get(i);
                assertThat(x.getSourceIdentifier()).isEqualTo(y.getSourceIdentifier());
                assertThat(x.getName()).isEqualTo(y.getName());
                assertThat(x.getLongitude()).isEqualTo(y.getLongitude());
                assertThat(x.getLatitude()).isEqualTo(y.getLatitude());
                assertThat(x.getDemandKg()).isEqualByComparingTo(y.getDemandKg());
                assertThat(x.getDemandBreakdown()).isEqualTo(y.getDemandBreakdown());
            }
            assertThat(a.getDepotName()).isEqualTo(b.getDepotName());
            assertThat(a.getVehicleCapacityKg()).isEqualByComparingTo(b.getVehicleCapacityKg());
            assertThat(a.getTotalDemandKg()).isEqualByComparingTo(b.getTotalDemandKg());
            assertThat(a.getMatrixJson()).isEqualTo(b.getMatrixJson());
            for (int m = 0; m < 3; m++) {
                MethodResult p = a.getResults().get(m);
                MethodResult q = b.getResults().get(m);
                assertThat(p.visitOrderList()).isEqualTo(q.visitOrderList());
                assertThat(p.getDistanceM()).isEqualTo(q.getDistanceM());
                assertThat(p.getTravelTimeS()).isEqualTo(q.getTravelTimeS());
                assertThat(p.getDurationS()).isEqualTo(q.getDurationS());
                assertThat(p.isFeasible()).isEqualTo(q.isFeasible());
                assertThat(p.isCoverageOk()).isEqualTo(q.isCoverageOk());
                assertThat(p.isCapacityOk()).isEqualTo(q.isCapacityOk());
                assertThat(p.getAlgorithmConfiguration()).isEqualTo(q.getAlgorithmConfiguration());
            }
        }
    }

    @Test
    @Transactional
    void goldenScenario() {
        EvaluationBatch b = experiments.run(spec(List.of(DemandProfile.UNIFORM_LOW), List.of(1L), List.of(10)), "admin1");
        EvaluationRun r = runsOf(b).get(0);
        assertThat(r.getStops().stream().map(EvaluationStop::getSourceIdentifier).toList()).containsExactly("10130030",
                "10130234", "10130154", "10130100", "10130238", "10130246", "10130245", "10130099", "10130034",
                "10130094");
        assertThat(r.getStops().stream().map(s -> s.getDemandKg().intValue()).toList())
                .containsExactly(21, 23, 12, 17, 26, 15, 29, 11, 19, 29);
        assertThat(r.getResults().get(0).visitOrderList()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(r.getResults().get(2).getDistanceM()).isLessThanOrEqualTo(r.getResults().get(1).getDistanceM());
    }

    @Test
    void missingImportIsConflictAndWritesNothing() {
        tours.deleteAll();
        points.deleteAll();
        ExperimentSpec tiny = spec(List.of(DemandProfile.UNIFORM_LOW), List.of(1L), List.of(3));
        assertThatThrownBy(() -> experiments.run(tiny, "admin1")).isInstanceOf(ConflictException.class);
        assertThat(jdbc.queryForObject("select count(*) from evaluation_batch", Long.class)).isZero();
        assertThat(evaluationRuns.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from audit_event where action = 'EVALUATION_EXPERIMENT_RUN'", Long.class)).isZero();
    }

    @Test
    void canonicalSpec() {
        ExperimentSpec c = ExperimentSpec.canonical();
        assertThat(c.isCanonical()).isTrue();
        assertThat(c.scenarioCount()).isEqualTo(60);
        assertThat(c.runCount()).isEqualTo(60);
        assertThat(c.profiles()).containsExactly(DemandProfile.values());
        assertThat(c.seeds()).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(c.sizes()).containsExactly(10, 25, 50, 100);
        assertThat(c.label()).isEqualTo("Fixed MN01 experiment");
        ExperimentSpec three = ExperimentSpec.of(c.profiles(), c.seeds(), c.sizes(), 3, null);
        assertThat(three.isCanonical()).isTrue();
        assertThat(three.runCount()).isEqualTo(180);
    }

    private void invalid(String field, List<DemandProfile> p, List<Long> s, List<Integer> z, Integer r, String label) {
        assertThatThrownBy(() -> ExperimentSpec.of(p, s, z, r, label)).isInstanceOfSatisfying(
                InvalidRequestException.class, e -> assertThat(e.getField()).isEqualTo(field));
    }

    @Test
    void validation() {
        invalid("profiles", List.of(DemandProfile.HOTSPOT, DemandProfile.HOTSPOT), null, null, null, null);
        invalid("profiles", List.of(), null, null, null, null);
        invalid("seeds", null, List.of(0L), null, null, null);
        List<Long> eleven = new ArrayList<>();
        for (long i = 1; i <= 11; i++) {
            eleven.add(i);
        }
        invalid("seeds", null, eleven, null, null, null);
        invalid("sizes", null, null, List.of(1), null, null);
        invalid("sizes", null, null, List.of(101), null, null);
        invalid("sizes", null, null, List.of(5, 5), null, null);
        invalid("repetitions", null, null, null, 0, null);
        invalid("repetitions", null, null, null, 4, null);
        invalid("label", null, null, null, null, "x".repeat(121));
        List<Long> ten = eleven.subList(0, 10);
        invalid("sizes", null, ten, List.of(2, 3, 4, 5, 6, 7, 8), null, null);
    }
}
