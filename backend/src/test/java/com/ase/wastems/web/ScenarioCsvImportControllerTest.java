package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.service.DemandProfile;
import com.ase.wastems.service.EvaluationExportService;
import com.ase.wastems.service.EvaluationService;
import com.ase.wastems.service.ExperimentService;
import com.ase.wastems.service.ExperimentSpec;
import com.ase.wastems.service.ImportTrigger;
import com.ase.wastems.service.Mn01ImportService;
import com.ase.wastems.service.SimpleCsv;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class ScenarioCsvImportControllerTest extends ExecutionTestSupport {

    static final String H = "scenario_key,input_order,point_id,source_dataset,source_identifier,longitude,latitude,"
            + "category,demand_kg,vehicle_capacity_kg,demand_profile,seed";

    @Autowired Mn01ImportService mn01Import;
    @Autowired ExperimentService experiments;
    @Autowired EvaluationService evaluationService;
    @Autowired EvaluationExportService exports;
    @Autowired JdbcTemplate jdbc;

    private final Map<String, double[]> coords = new java.util.HashMap<>();

    @BeforeEach
    void setUpImport() {
        jdbc.update("delete from evaluation_batch");
        jdbc.update("delete from audit_event");
        mn01Import.importAll(ImportTrigger.MANUAL);
        for (CollectionPoint p : points.findBySourceDataset("8znf-7b2c")) {
            coords.put(p.getSourceIdentifier(), new double[] {p.getLongitude(), p.getLatitude()});
        }
    }

    @AfterEach
    void cleanImport() {
        jdbc.update("delete from evaluation_batch");
        sourceSnapshots.deleteAll();
    }

    private String src(String key, int order, String id, String cat, String kg, String cap) {
        double[] c = coords.get(id);
        return key + "," + order + ",,8znf-7b2c," + id + "," + c[0] + "," + c[1] + "," + cat + "," + kg + "," + cap + ",,";
    }

    private ResultActions performImport(String csv, String fileName) throws Exception {
        String url = "/api/evaluations/imports" + (fileName == null ? "" : "?fileName=" + fileName);
        return mvc.perform(post(url).with(asPlanner()).with(csrf()).contentType("text/csv")
                .content(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private long[] counts() {
        return new long[] {
            jdbc.queryForObject("select count(*) from evaluation_batch", Long.class),
            jdbc.queryForObject("select count(*) from evaluation_run", Long.class),
            jdbc.queryForObject("select count(*) from audit_event where action = 'EVALUATION_SCENARIOS_IMPORTED'", Long.class)
        };
    }

    private void assertRefused(String csv, String... messages) throws Exception {
        long[] before = counts();
        String body = performImport(csv, "x.csv").andExpect(status().isBadRequest()).andReturn().getResponse()
                .getContentAsString();
        for (String m : messages) {
            assertThat(body).contains(m);
        }
        assertThat(counts()).containsExactly(before);
    }

    private List<String> pairs(String body) throws Exception {
        List<String> out = new ArrayList<>();
        mapper.readTree(body).get("rowErrors").forEach(e -> out.add(e.get("row").asInt() + ":"
                + (e.has("column") ? e.get("column").asText() : "-")));
        return out;
    }

    @Test
    void emptyBodyIsRejectedOnTheFileField() throws Exception {
        long[] before = counts();
        performImport("", "x.csv").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("file"));
        assertThat(counts()).containsExactly(before);
    }

    @Test
    void headerProblems() throws Exception {
        assertRefused(H.replace(",demand_kg", "") + "\nx,1,,8znf-7b2c,10130030,-74.0,40.7,Mixed litter,10000,,\n",
                "missing column demand_kg");
        long[] before = counts();
        String body = performImport(H + ",colour\n" + src("a", 1, "10130030", "Mixed litter", "5", "1000") + ",x\n", null)
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("unknown column colour");
        String noIdentity = "scenario_key,input_order,longitude,latitude,category,demand_kg,vehicle_capacity_kg\n"
                + "a,1,-74.0,40.7,Mixed litter,5,1000\n";
        assertRefused(noIdentity, "missing point identity columns: point_id or source_dataset and source_identifier");
        assertThat(counts()).containsExactly(before);
        JsonNode first = mapper.readTree(performImport(H.replace(",demand_kg", "") + "\nx\n", "x.csv").andReturn()
                .getResponse().getContentAsString()).get("rowErrors").get(0);
        assertThat(first.get("row").asInt()).isEqualTo(1);
        assertThat(first.get("column").asText()).isEqualTo("demand_kg");
    }

    @Test
    void validationMatrix() throws Exception {
        double[] c1 = coords.get("10130030");
        double[] nd = coords.get("10110163");
        String pre = "," + c1[0] + "," + c1[1] + ",Mixed litter,10,1000,,";
        String base = "matrix,1,,8znf-7b2c,10130030";
        String random = UUID.randomUUID().toString();
        List<String> rows = new ArrayList<>(List.of(H,
                "bad key!,1,,8znf-7b2c,10130030" + pre,                                   // 2
                "matrix,0,,8znf-7b2c,10130030" + pre,                                     // 3
                "matrix,x,,8znf-7b2c,10130030" + pre,                                     // 4
                base + ",abc," + c1[1] + ",Mixed litter,10,1000,,",                       // 5
                base + "," + c1[0] + ",95,Mixed litter,10,1000,,",                        // 6
                base + ",-80.0," + c1[1] + ",Mixed litter,10,1000,,",                     // 7
                base + "," + c1[0] + "," + c1[1] + ",Mixed litter,-1,1000,,",             // 8
                base + "," + c1[0] + "," + c1[1] + ",Mixed litter,1.234,1000,,",          // 9
                base + "," + c1[0] + "," + c1[1] + ",Mixed litter,10,0,,",                // 10
                base + "," + c1[0] + "," + c1[1] + ",Plastic,10,1000,,",                  // 11
                base + "," + c1[0] + "," + c1[1] + ",Old category,10,1000,,",             // 12
                "matrix,1," + random + ",,," + c1[0] + "," + c1[1] + ",Mixed litter,10,1000,,", // 13
                "matrix,1,,8znf-7b2c,99999999" + pre,                                     // 14
                "matrix,1," + random + ",8znf-7b2c,10130030" + pre,                       // 15
                base + "," + c1[0] + "," + (c1[1] + 0.001) + ",Mixed litter,10,1000,,",   // 16
                "matrix,1,,8znf-7b2c,10110163," + nd[0] + "," + nd[1] + ",Mixed litter,10,1000,,", // 17
                base + pre + ",extra"));                                                  // 18
        long[] before = counts();
        String body = performImport(String.join("\n", rows) + "\n", "m.csv").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The CSV has 17 errors; nothing was imported."))
                .andReturn().getResponse().getContentAsString();
        assertThat(pairs(body)).containsExactly("2:scenario_key", "3:input_order", "4:input_order", "5:longitude",
                "6:latitude", "7:longitude", "8:demand_kg", "9:demand_kg", "10:vehicle_capacity_kg", "11:category",
                "12:category", "13:point_id", "14:source_identifier", "15:-", "16:latitude",
                "17:source_identifier", "18:-");
        assertThat(body).contains("must match [A-Za-z0-9._-]{1,80}", "must be an integer >= 1", "must be a number",
                "must be between -90 and 90", "outside the service area (longitude and latitude may be swapped)",
                "must be > 0", "must have at most 2 decimals", "category Plastic not found",
                "category Old category is inactive", "point 8znf-7b2c/99999999 not found",
                "give either point_id or source_dataset/source_identifier, not both",
                "differs from the point's stored coordinate", "point 8znf-7b2c/10110163 is not a DSNY basket (BID-owned Compactor)",
                "expected 12 fields, found 13");
        assertThat(counts()).containsExactly(before);
    }

    @Test
    void crossRowRules() throws Exception {
        String csv = String.join("\n", H,
                src("dupcat", 1, "10130030", "Mixed litter", "5", "1000"),
                src("dupcat", 1, "10130030", "Mixed litter", "6", "1000"),
                src("duppoint", 1, "10130030", "Mixed litter", "5", "1000"),
                src("duppoint", 2, "10130030", "Mixed litter", "5", "1000"),
                src("gap", 1, "10130030", "Mixed litter", "5", "1000"),
                src("gap", 3, "10130234", "Mixed litter", "5", "1000"),
                src("caps", 1, "10130030", "Mixed litter", "5", "1000"),
                src("caps", 2, "10130234", "Mixed litter", "5", "2000")) + "\n";
        long[] before = counts();
        String body = performImport(csv, "c.csv").andExpect(status().isBadRequest()).andReturn().getResponse()
                .getContentAsString();
        assertThat(body).contains("category Mixed litter listed twice for input order 1",
                "point 8znf-7b2c/10130030 already listed at input order 1",
                "input orders of scenario gap must be 1..2 without gaps",
                "vehicle_capacity_kg differs within scenario caps", "\"The CSV has 4 errors; nothing was imported.\"");
        assertThat(counts()).containsExactly(before);
    }

    @Test
    void limits() throws Exception {
        List<String> ids = points.findBySourceDataset("8znf-7b2c").stream()
                .filter(p -> p.getSourceOwnershipType().startsWith("DSNY "))
                .map(CollectionPoint::getSourceIdentifier).sorted().toList();
        List<String> eleven = new ArrayList<>(List.of(H));
        for (int i = 0; i < 11; i++) {
            eleven.add(src("s" + i, 1, ids.get(i), "Mixed litter", "5", "1000"));
        }
        assertRefused(String.join("\n", eleven) + "\n", "more than 10 scenarios");
        List<String> big = new ArrayList<>(List.of(H));
        for (int i = 0; i < 101; i++) {
            big.add(src("big", i + 1, ids.get(i), "Mixed litter", "5", "1000"));
        }
        assertRefused(String.join("\n", big) + "\n", "scenario big has more than 100 points");
        List<String> bad = new ArrayList<>(List.of(H));
        for (int i = 0; i < 101; i++) {
            bad.add(src("bad", i + 1, ids.get(i), "Mixed litter", "-1", "1000"));
        }
        long[] before = counts();
        String body = performImport(String.join("\n", bad) + "\n", "b.csv").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.rowErrors.length()").value(100))
                .andExpect(jsonPath("$.message").value("The CSV has 101 errors (first 100 shown); nothing was imported."))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).isNotEmpty();
        assertThat(counts()).containsExactly(before);
    }

    @Test
    void successfulImportIsEvaluatedAndRetained() throws Exception {
        String csv = String.join("\n", H,
                "lower-broadway-3,1,,8znf-7b2c,10130067,-74.00212207,40.71528035,Mixed litter,18,10000,,",
                "lower-broadway-3,2,,8znf-7b2c,10130002,-74.00821676,40.71706895,Mixed litter,12.5,10000,,",
                "lower-broadway-3,3,,8znf-7b2c,10110232,-74.0115043,40.71560317,Mixed litter,22,10000,,",
                "two-categories,1,,8znf-7b2c,10130030,-74.00955505,40.72423185,Mixed litter,10,10,manual,42",
                "two-categories,1,,8znf-7b2c,10130030,-74.00955505,40.72423185,Paper,5,10,manual,42",
                "two-categories,2,,8znf-7b2c,10130234,-74.00149097,40.71711872,Mixed litter,10,10,manual,42") + "\n";
        MvcResult res = performImport(csv, "ex.csv").andExpect(status().isCreated())
                .andExpect(jsonPath("$.batch.kind").value("CSV_IMPORT"))
                .andExpect(jsonPath("$.batch.status").value("COMPLETED"))
                .andExpect(jsonPath("$.batch.scenarioCount").value(2))
                .andExpect(jsonPath("$.batch.runCount").value(2))
                .andExpect(jsonPath("$.batch.fileName").value("ex.csv"))
                .andExpect(jsonPath("$.batch.fileRowCount").value(6))
                .andExpect(jsonPath("$.batch.fileSha256")
                        .value(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(csv.getBytes(StandardCharsets.UTF_8)))))
                .andReturn();
        String batchId = mapper.readTree(res.getResponse().getContentAsString()).get("batch").get("id").asText();
        assertThat(res.getResponse().getHeader("Location")).endsWith("/api/evaluations/batches/" + batchId);
        JsonNode lb = runByKey(batchId, "lower-broadway-3");
        assertThat(lb.get("stops")).hasSize(3);
        assertThat(lb.get("stops").get(0).get("sourceIdentifier").asText()).isEqualTo("10130067");
        assertThat(lb.get("stops").get(1).get("demandKg").asDouble()).isEqualTo(12.5);
        assertThat(lb.get("stops").get(2).get("demandKg").asDouble()).isEqualTo(22.0);
        lb.get("results").forEach(r -> assertThat(r.get("feasible").asBoolean()).isTrue());
        assertThat(lb.get("sourceSnapshotIds").get(0).asText()).isEqualTo("8znf-7b2c:449:a0737237e370");
        assertThat(lb.get("sourceSnapshotIds").get(1).asText()).isEqualTo("xw3j-2yxf:1:207851c2e229");
        JsonNode tc = runByKey(batchId, "two-categories");
        assertThat(tc.get("stops").get(0).get("demandKg").asDouble()).isEqualTo(15.0);
        assertThat(tc.get("stops").get(0).get("demandBreakdown").asText()).isEqualTo("Mixed litter=10.00; Paper=5.00");
        assertThat(tc.get("demandProfile").asText()).isEqualTo("manual");
        assertThat(tc.get("seed").asInt()).isEqualTo(42);
        tc.get("results").forEach(r -> {
            assertThat(r.get("feasible").asBoolean()).isFalse();
            assertThat(r.get("capacityOk").asBoolean()).isFalse();
        });
        assertThat(jdbc.queryForList("select outcome from audit_event where action = 'EVALUATION_SCENARIOS_IMPORTED'",
                String.class)).containsExactly("SUCCESS");
    }

    private JsonNode runByKey(String batchId, String key) throws Exception {
        JsonNode list = mapper.readTree(mvc.perform(get("/api/evaluations?batchId=" + batchId).with(asPlanner()))
                .andReturn().getResponse().getContentAsString()).get("items");
        for (JsonNode i : list) {
            if (key.equals(i.get("scenarioKey").asText())) {
                return mapper.readTree(mvc.perform(get("/api/evaluations/" + i.get("id").asText()).with(asPlanner()))
                        .andReturn().getResponse().getContentAsString());
            }
        }
        throw new AssertionError("no run " + key);
    }

    @Test
    void routingFailureIsRetained() throws Exception {
        CollectionPoint queens = points.save(new CollectionPoint("Queens test", -73.80, 40.70, DataOrigin.USER_ENTERED));
        double[] c = coords.get("10130030");
        String csv = String.join("\n", H,
                "q,1," + queens.getId() + ",,,-73.8,40.7,Mixed litter,10,1000,,",
                "q,2,,8znf-7b2c,10130030," + c[0] + "," + c[1] + ",Mixed litter,10,1000,,") + "\n";
        MvcResult res = performImport(csv, "q.csv").andExpect(status().isCreated())
                .andExpect(jsonPath("$.batch.failedRunCount").value(1)).andReturn();
        String batchId = mapper.readTree(res.getResponse().getContentAsString()).get("batch").get("id").asText();
        JsonNode run = runByKey(batchId, "q");
        assertThat(run.get("status").asText()).isEqualTo("ROUTING_FAILED");
        assertThat(run.get("failureCode").asText()).isEqualTo("POINT_NOT_ROUTABLE");
        assertThat(jdbc.queryForList("select outcome from audit_event where action = 'EVALUATION_SCENARIOS_IMPORTED'",
                String.class)).containsExactly("FAILURE");
    }

    @Test
    void roundTripGivesTheSameResults() throws Exception {
        EvaluationBatch exp = experiments.run(ExperimentSpec.of(List.of(DemandProfile.UNIFORM_LOW, DemandProfile.HOTSPOT),
                List.of(2L), List.of(5, 9), null, null), "admin1");
        String scenarios = mvc.perform(get("/api/evaluations/batches/" + exp.getId() + "/scenarios.csv").with(asPlanner()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        MvcResult res = performImport(scenarios, "rt.csv").andExpect(status().isCreated())
                .andExpect(jsonPath("$.batch.runCount").value(4)).andReturn();
        UUID imported = UUID.fromString(mapper.readTree(res.getResponse().getContentAsString()).get("batch").get("id").asText());
        Map<String, EvaluationRun> a = loaded(exp.getId());
        Map<String, EvaluationRun> b = loaded(imported);
        assertThat(b.keySet()).isEqualTo(a.keySet());
        for (String key : a.keySet()) {
            EvaluationRun x = a.get(key);
            EvaluationRun y = b.get(key);
            assertThat(y.getScenarioKind().name()).isEqualTo("CSV_IMPORT");
            assertThat(y.getStops()).hasSameSizeAs(x.getStops());
            for (int i = 0; i < x.getStops().size(); i++) {
                assertThat(y.getStops().get(i).getSourceIdentifier()).isEqualTo(x.getStops().get(i).getSourceIdentifier());
                assertThat(y.getStops().get(i).getLongitude()).isEqualTo(x.getStops().get(i).getLongitude());
                assertThat(y.getStops().get(i).getLatitude()).isEqualTo(x.getStops().get(i).getLatitude());
                assertThat(y.getStops().get(i).getDemandKg()).isEqualByComparingTo(x.getStops().get(i).getDemandKg());
            }
            assertThat(y.getMatrixJson()).isEqualTo(x.getMatrixJson());
            for (int m = 0; m < 3; m++) {
                MethodResult p = x.getResults().get(m);
                MethodResult q = y.getResults().get(m);
                assertThat(q.visitOrderList()).isEqualTo(p.visitOrderList());
                assertThat(q.getDistanceM()).isEqualTo(p.getDistanceM());
                assertThat(q.getDurationS()).isEqualTo(p.getDurationS());
            }
        }
        Map<String, Map<String, String>> ra = byKeyMethod(exports.resultsCsv(null, exp.getId(), null));
        Map<String, Map<String, String>> rb = byKeyMethod(exports.resultsCsv(null, imported, null));
        assertThat(rb.keySet()).isEqualTo(ra.keySet());
        for (String k : ra.keySet()) {
            for (String col : List.of("distance_m", "travel_time_s", "duration_s", "visit_order",
                    "visit_order_source_ids", "feasible", "distance_change_pct")) {
                assertThat(rb.get(k).get(col)).as(k + " " + col).isEqualTo(ra.get(k).get(col));
            }
        }
    }

    private Map<String, EvaluationRun> loaded(UUID batchId) {
        List<UUID> ids = evaluationRuns.findByBatchIdOrderByCreatedAtAscIdAsc(batchId).stream().map(EvaluationRun::getId)
                .toList();
        return ids.stream().map(evaluationService::get).collect(Collectors.toMap(EvaluationRun::getScenarioKey, r -> r));
    }

    private static Map<String, Map<String, String>> byKeyMethod(String csv) {
        return SimpleCsv.parse(csv).stream().collect(Collectors.toMap(r -> r.get("scenario_key") + "/" + r.get("method"), r -> r));
    }

    @Test
    void rolesAndMedia() throws Exception {
        String csv = String.join("\n", H, src("a", 1, "10130030", "Mixed litter", "5", "1000")) + "\n";
        long[] before = counts();
        mvc.perform(post("/api/evaluations/imports").with(asAgent("agent1")).with(csrf()).contentType("text/csv")
                .content(csv)).andExpect(status().isForbidden());
        mvc.perform(post("/api/evaluations/imports").with(csrf()).contentType("text/csv").content(csv))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/evaluations/imports").with(asPlanner()).contentType("text/csv").content(csv))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/evaluations/imports").with(asPlanner()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(csv)).andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/api/evaluations/imports?label=" + "x".repeat(121)).with(asPlanner()).with(csrf())
                .contentType("text/csv").content(csv)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("label"));
        assertThat(counts()).containsExactly(before);
    }
}
