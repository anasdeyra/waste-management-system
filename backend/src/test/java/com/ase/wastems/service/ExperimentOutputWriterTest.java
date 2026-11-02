package com.ase.wastems.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.TourRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ExperimentOutputWriterTest {

    @TempDir Path dir;
    @Autowired ExperimentOutputWriter writer;
    @Autowired ExperimentService experiments;
    @Autowired EvaluationRunRepository evaluationRuns;
    @Autowired CollectionPointRepository points;
    @Autowired DepotRepository depots;
    @Autowired TourRepository tours;
    @Autowired SourceSnapshotRepository snapshots;
    @Autowired Mn01ImportService mn01Import;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

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
    void writesThreeFiles() throws Exception {
        EvaluationBatch b = experiments.run(
                ExperimentSpec.of(List.of(DemandProfile.UNIFORM_HIGH), List.of(1L), List.of(3), null, null), "admin1");
        writer.write(b.getId(), dir.resolve("out"));
        Path out = dir.resolve("out");
        assertThat(Files.readAllLines(out.resolve("results.csv"))).hasSize(5);
        assertThat(Files.readAllLines(out.resolve("scenarios.csv"))).hasSize(4);
        JsonNode json = mapper.readTree(out.resolve("batch.json").toFile());
        assertThat(json.get("id").asText()).isEqualTo(b.getId().toString());
        assertThat(json.get("runCount").asInt()).isEqualTo(1);
        assertThat(json.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(json.get("aggregates").size()).isGreaterThan(0);
        assertThat(json.get("scenarioKeys").get(0).asText()).isEqualTo("mn01-UNIFORM_HIGH-s1-n3");
    }
}
