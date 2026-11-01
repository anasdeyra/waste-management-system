package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.service.DemandProfile;
import com.ase.wastems.service.EvaluationExportService;
import com.ase.wastems.service.ExperimentService;
import com.ase.wastems.service.ExperimentSpec;
import com.ase.wastems.service.ImportTrigger;
import com.ase.wastems.service.Mn01ImportService;
import com.ase.wastems.service.SimpleCsv;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

class EvaluationExportControllerTest extends ExecutionTestSupport {

    @Autowired Mn01ImportService mn01Import;
    @Autowired ExperimentService experiments;
    @Autowired JdbcTemplate jdbc;

    private UUID batchId;
    private String tourRunId;

    @BeforeEach
    void setUpExport() throws Exception {
        jdbc.update("delete from evaluation_batch");
        mn01Import.importAll(ImportTrigger.MANUAL);
        UUID tour = doCreateDraft();
        tourRunId = mapper.readTree(mvc.perform(post("/api/tours/" + tour + "/evaluations").with(asPlanner()).with(csrf()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        EvaluationBatch b = experiments.run(
                ExperimentSpec.of(List.of(DemandProfile.HOTSPOT), List.of(5L), List.of(4, 6), null, null), "admin1");
        batchId = b.getId();
    }

    @AfterEach
    void cleanExport() {
        jdbc.update("delete from evaluation_batch");
        sourceSnapshots.deleteAll();
    }

    private JsonNode runDetail(int pointCount) throws Exception {
        String list = mvc.perform(get("/api/evaluations?batchId=" + batchId + "&pointCount=" + pointCount).with(asPlanner()))
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(list).get("items").get(0).get("id").asText();
        return mapper.readTree(mvc.perform(get("/api/evaluations/" + id).with(asPlanner())).andReturn().getResponse()
                .getContentAsString());
    }

    private MvcResult performExport(String url) throws Exception {
        return mvc.perform(get(url).with(asPlanner())).andExpect(status().isOk()).andReturn();
    }

    private static List<Map<String, String>> parse(MvcResult r) throws Exception {
        return SimpleCsv.parse(r.getResponse().getContentAsString());
    }

    private static double d(Map<String, String> row, String col) {
        return Double.parseDouble(row.get(col));
    }

    @Test
    void resultsOfABatch() throws Exception {
        MvcResult res = performExport("/api/evaluations/export?batchId=" + batchId);
        assertThat(res.getResponse().getContentType()).startsWith("text/csv");
        assertThat(res.getResponse().getHeader("Content-Disposition")).contains("attachment")
                .contains("evaluation-results-batch-" + batchId + ".csv");
        String text = res.getResponse().getContentAsString();
        assertThat(text.lines().findFirst().orElseThrow()).isEqualTo(String.join(",", EvaluationExportService.RESULT_COLUMNS));
        List<List<String>> records = SimpleCsv.records(text);
        assertThat(records).hasSize(7);
        assertThat(records).allMatch(r -> r.size() == EvaluationExportService.RESULT_COLUMNS.size());
        List<Map<String, String>> rows = SimpleCsv.parse(text);
        assertThat(rows).hasSize(6);
        for (int i = 0; i < rows.size(); i += 3) {
            assertThat(rows.get(i).get("method")).isEqualTo("INPUT_ORDER");
            assertThat(rows.get(i + 1).get("method")).isEqualTo("NEAREST_NEIGHBOR");
            assertThat(rows.get(i + 2).get("method")).isEqualTo("NN_PLUS_2_OPT");
        }
        for (Map<String, String> row : rows) {
            assertThat(row.get("feasible")).isEqualTo("true");
            double io = d(row, "input_order_distance_m");
            assertThat((d(row, "distance_m") - io) / io * 100).isCloseTo(d(row, "distance_change_pct"), within(0.001));
            double iod = d(row, "input_order_duration_s");
            assertThat((d(row, "duration_s") - iod) / iod * 100).isCloseTo(d(row, "duration_change_pct"), within(0.001));
            assertThat(d(row, "duration_s")).isCloseTo(d(row, "travel_time_s") + d(row, "service_time_total_s"), within(0.002));
            assertThat(row.get("source_snapshot_ids")).isEqualTo("8znf-7b2c:449:a0737237e370;xw3j-2yxf:1:207851c2e229");
            assertThat(row.get("scenario_kind")).isEqualTo("EXPERIMENT");
            assertThat(row.get("demand_profile")).isEqualTo("HOTSPOT");
            assertThat(row.get("seed")).isEqualTo("5");
            assertThat(row.get("repetition")).isEqualTo("1");
        }
        assertThat(rows.get(0).get("point_count")).isEqualTo("4");
        assertThat(rows.get(3).get("point_count")).isEqualTo("6");
        assertThat(rows.get(0).get("visit_order")).isEqualTo("1 2 3 4");
        List<String> expected = new java.util.ArrayList<>();
        runDetail(4).get("stops").forEach(s -> expected.add(s.get("sourceIdentifier").asText()));
        assertThat(List.of(rows.get(0).get("visit_order_source_ids").split(" "))).isEqualTo(expected);
    }

    @Test
    void filtersAndNames() throws Exception {
        MvcResult tour = performExport("/api/evaluations/export?kind=TOUR_DRAFT");
        List<Map<String, String>> rows = parse(tour);
        assertThat(rows).hasSize(3);
        assertThat(rows).allMatch(r -> !r.get("tour_id").isEmpty() && r.get("scenario_key").isEmpty());
        MvcResult one = performExport("/api/evaluations/export?runId=" + tourRunId);
        assertThat(parse(one)).hasSize(3);
        assertThat(one.getResponse().getHeader("Content-Disposition")).contains("evaluation-results-run-" + tourRunId + ".csv");
        assertThat(parse(performExport("/api/evaluations/export"))).hasSize(9);
        MvcResult unknown = performExport("/api/evaluations/export?batchId=" + UUID.randomUUID());
        assertThat(parse(unknown)).isEmpty();
        assertThat(unknown.getResponse().getContentAsString().lines().count()).isEqualTo(1);
    }

    @Test
    void noOperationalColumns() {
        for (String c : EvaluationExportService.RESULT_COLUMNS) {
            assertThat(c).doesNotContain("vehicle_id").doesNotContain("agent").doesNotContain("user")
                    .doesNotContain("password").doesNotContain("category");
        }
    }

    @Test
    void scenariosInImportFormat() throws Exception {
        MvcResult res = performExport("/api/evaluations/batches/" + batchId + "/scenarios.csv");
        assertThat(res.getResponse().getHeader("Content-Disposition")).contains("evaluation-scenarios-" + batchId + ".csv");
        String text = res.getResponse().getContentAsString();
        assertThat(text.lines().findFirst().orElseThrow()).isEqualTo(String.join(",", EvaluationExportService.SCENARIO_COLUMNS));
        List<Map<String, String>> rows = SimpleCsv.parse(text);
        assertThat(rows).hasSize(10);
        assertThat(rows).allMatch(r -> r.get("point_id").isEmpty() && r.get("source_dataset").equals("8znf-7b2c")
                && r.get("category").equals("Mixed litter") && r.get("vehicle_capacity_kg").equals("10000.00")
                && r.get("demand_profile").equals("HOTSPOT") && r.get("seed").equals("5"));
        JsonNode stops = runDetail(4).get("stops");
        for (int i = 0; i < 4; i++) {
            assertThat(rows.get(i).get("demand_kg")).isEqualTo(
                    stops.get(i).get("demandBreakdown").asText().substring("Mixed litter=".length()));
        }
        mvc.perform(get("/api/evaluations/batches/" + UUID.randomUUID() + "/scenarios.csv").with(asPlanner()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rolesAreEnforced() throws Exception {
        for (String url : List.of("/api/evaluations/export", "/api/evaluations/batches/" + batchId + "/scenarios.csv")) {
            mvc.perform(get(url).with(asAgent("agent1"))).andExpect(status().isForbidden());
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
    }
}
