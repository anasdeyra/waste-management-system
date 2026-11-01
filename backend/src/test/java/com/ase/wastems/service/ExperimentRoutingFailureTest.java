package com.ase.wastems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.routing.DirectedCostMatrixProvider;
import com.ase.wastems.routing.RoadLegUnavailableException;
import com.ase.wastems.routing.RoadLegUnavailableException.Reason;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class ExperimentRoutingFailureTest {

    @MockitoSpyBean DirectedCostMatrixProvider matrices;
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

    @Test
    @Transactional
    void failedScenarioIsRetainedAndTheBatchContinues() {
        doThrow(new RoadLegUnavailableException(Reason.NO_ROAD_PATH, 1, 2, "test: no road path"))
                .when(matrices).buildMatrix(argThat(n -> n != null && n.size() == 4));
        EvaluationBatch b = experiments.run(
                ExperimentSpec.of(List.of(DemandProfile.UNIFORM_LOW), List.of(1L), List.of(3, 5), null, null), "admin1");
        assertThat(b.getCompletedRunCount()).isEqualTo(1);
        assertThat(b.getFailedRunCount()).isEqualTo(1);
        List<EvaluationRun> runs = evaluationRuns.findAll().stream().filter(r -> b.getId().equals(r.getBatchId())).toList();
        EvaluationRun failed = runs.stream().filter(r -> r.getScenarioKey().equals("mn01-UNIFORM_LOW-s1-n3")).findFirst().orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(EvaluationStatus.ROUTING_FAILED);
        assertThat(failed.getFailureCode()).isEqualTo("NO_ROAD_PATH");
        assertThat(failed.getResults()).isEmpty();
        assertThat(failed.getStops()).hasSize(3);
        EvaluationRun ok = runs.stream().filter(r -> r.getScenarioKey().equals("mn01-UNIFORM_LOW-s1-n5")).findFirst().orElseThrow();
        assertThat(ok.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
        assertThat(ok.getResults()).hasSize(3);
        assertThat(jdbc.queryForObject(
                        "select outcome from audit_event where action = 'EVALUATION_EXPERIMENT_RUN'", String.class))
                .isEqualTo("FAILURE");
    }
}
