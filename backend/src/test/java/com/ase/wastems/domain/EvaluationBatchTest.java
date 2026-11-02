package com.ase.wastems.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvaluationBatchTest {

    private EvaluationBatch experiment() {
        return EvaluationBatch.startExperiment("l", false, "p", List.of("UNIFORM_LOW", "HOTSPOT"), List.of(1L, 2L),
                List.of(10, 25), 1, BigDecimal.TEN, List.of("a", "b"), "v", "e", "env", 4, 4, "admin1",
                Instant.now());
    }

    @Test
    void experimentStartsRunningWithRoundTrippingLists() {
        EvaluationBatch b = experiment();
        assertThat(b.getStatus()).isEqualTo(BatchStatus.RUNNING);
        assertThat(b.getId()).isNotNull();
        assertThat(b.demandProfileList()).containsExactly("UNIFORM_LOW", "HOTSPOT");
        assertThat(b.seedList()).containsExactly(1L, 2L);
        assertThat(b.sizeList()).containsExactly(10, 25);
        assertThat(b.sourceSnapshotIdList()).containsExactly("a", "b");
    }

    @Test
    void recordsRunsAndFinishes() {
        EvaluationBatch b = experiment();
        b.recordRun(EvaluationStatus.COMPLETED);
        b.recordRun(EvaluationStatus.COMPLETED);
        b.recordRun(EvaluationStatus.ROUTING_FAILED);
        assertThat(b.getCompletedRunCount()).isEqualTo(2);
        assertThat(b.getFailedRunCount()).isEqualTo(1);
        Instant at = Instant.now();
        b.finish(at);
        assertThat(b.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(b.getFinishedAt()).isEqualTo(at);
    }

    @Test
    void abortCutsTheMessage() {
        EvaluationBatch b = experiment();
        b.abort("x".repeat(1500), Instant.now());
        assertThat(b.getStatus()).isEqualTo(BatchStatus.ABORTED);
        assertThat(b.getFailureMessage()).hasSize(1000);
    }

    @Test
    void csvImportIsNeverCanonical() {
        EvaluationBatch b = EvaluationBatch.startCsvImport("l", "f.csv", "abc", 5, List.of("a"), "v", "e", "env", 2,
                "planner1", Instant.now());
        assertThat(b.isCanonical()).isFalse();
        assertThat(b.getRunCount()).isEqualTo(b.getScenarioCount());
        assertThat(b.getKind()).isEqualTo(ScenarioKind.CSV_IMPORT);
    }
}
