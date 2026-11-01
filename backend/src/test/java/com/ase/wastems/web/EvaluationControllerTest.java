package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.SourceSnapshot;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.repository.VehicleRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Integration tests of the evaluation API (S5) on the real GraphHopper router and MN01 snapshot. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvaluationControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private TourRepository tours;
    @Autowired private EvaluationRunRepository evaluationRuns;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;
    @Autowired private VehicleRepository vehicles;
    @Autowired private CollectionPointRepository points;
    @Autowired private DepotRepository depots;
    @Autowired private WasteCategoryRepository categories;
    @Autowired private SourceSnapshotRepository sourceSnapshots;

    private Depot mn01g;
    private CollectionPoint p1;
    private CollectionPoint p2;
    private CollectionPoint p3;
    private CollectionPoint queens;
    private Vehicle big;
    private Vehicle tiny;
    private Agent dana;
    private Agent eli;
    private String basketSnapshotId;

    @BeforeEach
    void setUp() {
        wipe();
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
        WasteCategory paper = categories.save(new WasteCategory("Paper", null));
        mn01g = depots.save(new Depot("MN01G", -74.0104183829902, 40.7255225360961, DataOrigin.IMPORTED_REAL,
                "xw3j-2yxf", "MN01G"));
        p1 = basket(paper, "CENTRE STREET & WORTH STREET", -74.00212207, 40.71528035, "10130067");
        p2 = basket(paper, "BROADWAY & LEONARD STREET", -74.0040862, 40.71705309, "10130182");
        p3 = basket(paper, "BROADWAY & THOMAS STREET", -74.00523497, 40.71570284, "10130187");
        CollectionPoint q = new CollectionPoint("Queens test point", -73.80, 40.70, DataOrigin.USER_ENTERED);
        q.addDemand(new DemandEntry(paper, new BigDecimal("10.00"), DataOrigin.USER_ENTERED));
        queens = points.save(q);
        Vehicle vb = new Vehicle("EV-BIG", new BigDecimal("1000.00"), DataOrigin.USER_ENTERED);
        big = vehicles.save(vb);
        tiny = vehicles.save(new Vehicle("EV-TINY", new BigDecimal("15.00"), DataOrigin.USER_ENTERED));
        dana = agents.save(new Agent("Dana", DataOrigin.USER_ENTERED));
        eli = agents.save(new Agent("Eli", DataOrigin.USER_ENTERED));
        basketSnapshotId = sourceSnapshots.findBySourceDataset("8znf-7b2c")
                .orElseGet(() -> sourceSnapshots.save(new SourceSnapshot("8znf-7b2c", "data/nyc-mn01-baskets.csv",
                        449, "a0737237e370".repeat(5) + "abcd", "8znf-7b2c:449:a0737237e370", Instant.now())))
                .getSnapshotId();
    }

    private CollectionPoint basket(WasteCategory paper, String name, double lon, double lat, String id) {
        CollectionPoint p = new CollectionPoint(name, lon, lat, DataOrigin.IMPORTED_REAL);
        p.setSource("8znf-7b2c", id, "DSNY PSR");
        p.addDemand(new DemandEntry(paper, new BigDecimal("10.00"), DataOrigin.USER_ENTERED));
        return points.save(p);
    }

    @AfterEach
    void tearDown() {
        wipe();
    }

    private void wipe() {
        evaluationRuns.deleteAll();
        tours.deleteAll();
        points.deleteAll();
        depots.deleteAll();
        vehicles.deleteAll();
        categories.deleteAll();
        agents.deleteAll();
        users.deleteAll();
    }

    private static RequestPostProcessor asAdmin() {
        return user("admin1").roles("ADMIN");
    }

    private static RequestPostProcessor asPlanner() {
        return user("planner1").roles("PLANNER");
    }

    private static RequestPostProcessor asAgent() {
        return user("agent1").roles("AGENT");
    }

    private String doCreateTour() throws Exception {
        MvcResult r = mvc.perform(post("/api/tours").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planningDate\":\"" + LocalDate.now().plusDays(1) + "\",\"depotId\":\"" + mn01g.getId() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private void doAddStop(String tourId, CollectionPoint p) throws Exception {
        mvc.perform(post("/api/tours/" + tourId + "/stops").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"collectionPointId\":\"" + p.getId() + "\"}")).andExpect(status().isCreated());
    }

    private void doAssign(String tourId, Vehicle v) throws Exception {
        mvc.perform(put("/api/tours/" + tourId + "/assignment").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehicleId\":\"" + v.getId() + "\",\"driverId\":\"" + dana.getId()
                        + "\",\"collectorIds\":[\"" + eli.getId() + "\"]}")).andExpect(status().isOk());
    }

    private String readyTour(CollectionPoint... pts) throws Exception {
        String id = doCreateTour();
        for (CollectionPoint p : pts) {
            doAddStop(id, p);
        }
        doAssign(id, big);
        return id;
    }

    private ResultActions performEvaluate(String tourId) throws Exception {
        return mvc.perform(post("/api/tours/" + tourId + "/evaluations").with(asPlanner()).with(csrf()));
    }

    private String doGetBody(String path) throws Exception {
        return mvc.perform(get(path).with(asPlanner())).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
    }

    private String tourState(String tourId) throws Exception {
        JsonNode t = mapper.readTree(doGetBody("/api/tours/" + tourId));
        List<String> stopIds = new ArrayList<>();
        t.get("stops").forEach(s -> stopIds.add(s.get("id").asText()));
        return t.get("revision") + "|" + t.get("updatedAt") + "|" + stopIds + "|" + t.get("vehicle").get("id")
                + "|" + t.get("driver").get("id");
    }

    @Test
    void roleMatrix() throws Exception {
        String id = readyTour(p1, p2);
        mvc.perform(post("/api/tours/" + id + "/evaluations").with(asAgent()).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tours/" + id + "/evaluations").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tours/" + id + "/evaluations").with(asPlanner())).andExpect(status().isForbidden());
        mvc.perform(get("/api/evaluations/" + UUID.randomUUID()).with(asAgent())).andExpect(status().isForbidden());
        mvc.perform(post("/api/tours/" + id + "/evaluations").with(asAdmin()).with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    void evaluatesReadyTourWithThreeMethods() throws Exception {
        String id = readyTour(p1, p2, p3);
        String revisionBefore = String.valueOf(mapper.readTree(doGetBody("/api/tours/" + id)).get("revision"));
        MvcResult res = performEvaluate(id).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.tourRevision").value(Integer.parseInt(revisionBefore)))
                .andExpect(jsonPath("$.roadGraphVersion").value("osm-mn01-e1f2fabaa937"))
                .andExpect(jsonPath("$.routingEngine").value(Matchers.startsWith("GraphHopper 11.1")))
                .andExpect(jsonPath("$.sourceSnapshotIds.length()").value(2))
                .andExpect(jsonPath("$.sourceSnapshotIds[0]").value(basketSnapshotId))
                .andExpect(jsonPath("$.sourceSnapshotIds[1]").value(Matchers.startsWith("xw3j-2yxf:")))
                .andExpect(jsonPath("$.pointCount").value(3))
                .andExpect(jsonPath("$.totalDemandKg").value(30.0))
                .andExpect(jsonPath("$.serviceTimeSeconds").value(600.0))
                .andExpect(jsonPath("$.results[*].method")
                        .value(Matchers.contains("INPUT_ORDER", "NEAREST_NEIGHBOR", "NN_PLUS_2_OPT")))
                .andExpect(jsonPath("$.stops[0].name").value("CENTRE STREET & WORTH STREET"))
                .andExpect(jsonPath("$.stops[0].longitude").value(-74.00212207))
                .andReturn();
        String body = res.getResponse().getContentAsString();
        JsonNode run = mapper.readTree(body);
        assertThat(res.getResponse().getHeader("Location")).endsWith("/api/evaluations/" + run.get("id").asText());
        assertThat(run.get("createdBy").asText()).isEqualTo("planner1");
        JsonNode results = run.get("results");
        assertThat(results).hasSize(3);
        for (JsonNode r : results) {
            assertThat(r.get("feasible").asBoolean()).isTrue();
            assertThat(r.get("coverageOk").asBoolean()).isTrue();
            assertThat(r.get("capacityOk").asBoolean()).isTrue();
            List<Integer> order = new ArrayList<>();
            r.get("visitOrder").forEach(n -> order.add(n.asInt()));
            assertThat(order).containsExactlyInAnyOrder(1, 2, 3);
            assertThat(r.get("collectionPointOrder")).hasSize(3);
            assertThat(r.get("durationS").asDouble())
                    .isCloseTo(r.get("travelTimeS").asDouble() + 1800, org.assertj.core.api.Assertions.within(1e-6));
            JsonNode features = r.get("legs").get("features");
            assertThat(features).hasSize(4);
            double sum = 0;
            for (JsonNode f : features) {
                sum += f.get("properties").get("distanceM").asDouble();
                for (JsonNode c : f.get("geometry").get("coordinates")) {
                    assertThat(c.get(0).asDouble()).isLessThan(-73.9);
                    assertThat(c.get(1).asDouble()).isGreaterThan(40.6);
                }
            }
            assertThat(sum).isCloseTo(r.get("distanceM").asDouble(), org.assertj.core.api.Assertions.within(1e-3));
            assertThat(features.get(0).get("properties").get("fromIndex").asInt()).isZero();
            assertThat(features.get(3).get("properties").get("toIndex").asInt()).isZero();
            assertThat(r.get("executionTimeMs").asDouble()).isGreaterThanOrEqualTo(0);
        }
        assertThat(results.get(0).get("visitOrder").toString()).isEqualTo("[1,2,3]");
        assertThat(results.get(2).get("distanceM").asDouble())
                .isLessThanOrEqualTo(results.get(1).get("distanceM").asDouble());
    }

    @Test
    void evaluationNeverChangesTheDraft() throws Exception {
        String id = readyTour(p1, p2, p3);
        String before = tourState(id);
        performEvaluate(id).andExpect(status().isCreated());
        assertThat(tourState(id)).isEqualTo(before);
    }

    @Test
    void runIsRetrievable() throws Exception {
        String id = readyTour(p1, p2, p3);
        String body = performEvaluate(id).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        JsonNode post = mapper.readTree(body);
        String runId = post.get("id").asText();
        JsonNode detail = mapper.readTree(doGetBody("/api/evaluations/" + runId));
        for (int i = 0; i < 3; i++) {
            assertThat(detail.get("results").get(i).get("distanceM")).isEqualTo(post.get("results").get(i).get("distanceM"));
            assertThat(detail.get("results").get(i).get("visitOrder"))
                    .isEqualTo(post.get("results").get(i).get("visitOrder"));
        }
        JsonNode list = mapper.readTree(doGetBody("/api/tours/" + id + "/evaluations"));
        assertThat(list).hasSize(1);
        assertThat(list.get(0).get("id").asText()).isEqualTo(runId);
        assertThat(list.get(0).get("results")).hasSize(3);
        JsonNode mx = mapper.readTree(doGetBody("/api/evaluations/" + runId + "/matrix"));
        assertThat(mx.get("nodeCount").asInt()).isEqualTo(4);
        assertThat(mx.get("nodeLabels").get(0).asText()).startsWith("Depot: MN01G");
        assertThat(mx.get("distanceM").get(1).get(2).asDouble()).isBetween(250.0, 400.0);
        assertThat(mx.get("distanceM").get(2).get(1).asDouble()).isBetween(650.0, 950.0);
        for (int i = 0; i < 4; i++) {
            assertThat(mx.get("distanceM").get(i).get(i).asDouble()).isZero();
        }
    }

    @Test
    void notReadyTourIs422AndRetainsNothing() throws Exception {
        String id = doCreateTour();
        doAddStop(id, p1);
        performEvaluate(id).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("NO_VEHICLE")))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("NO_DRIVER")));
        assertThat(evaluationRuns.countByTourId(UUID.fromString(id))).isZero();
    }

    @Test
    void overCapacityIs422() throws Exception {
        String id = doCreateTour();
        doAddStop(id, p1);
        doAddStop(id, p2);
        doAssign(id, tiny);
        String before = tourState(id);
        performEvaluate(id).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("OVER_CAPACITY")))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("exceeds vehicle capacity")));
        assertThat(evaluationRuns.countByTourId(UUID.fromString(id))).isZero();
        assertThat(tourState(id)).isEqualTo(before);
    }

    @Test
    void unroutablePointIsRetainedAsRoutingFailure() throws Exception {
        String id = readyTour(p1, queens);
        String before = tourState(id);
        String body = performEvaluate(id).andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("ROUTING_FAILED"))
                .andExpect(jsonPath("$.failureCode").value("POINT_NOT_ROUTABLE"))
                .andExpect(jsonPath("$.failureMessage").value(Matchers.containsString("stop 2 (Queens test point)")))
                .andExpect(jsonPath("$.failureMessage").value(Matchers.containsString("limit 100 m")))
                .andExpect(jsonPath("$.results").isEmpty())
                .andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(body, "$.id");
        mvc.perform(get("/api/evaluations/" + runId + "/matrix").with(asPlanner())).andExpect(status().isNotFound());
        assertThat(tourState(id)).isEqualTo(before);
        mvc.perform(get("/api/tours/" + id + "/evaluations").with(asPlanner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ROUTING_FAILED"));
    }

    @Test
    void frozenScenarioIgnoresLaterPointEdits() throws Exception {
        String id = readyTour(p1, p2);
        String runId = JsonPath.read(
                performEvaluate(id).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(),
                "$.id");
        CollectionPoint edited = points.findById(p1.getId()).orElseThrow();
        edited.setName("Renamed");
        edited.setLongitude(-74.0030);
        points.save(edited);
        mvc.perform(get("/api/evaluations/" + runId).with(asPlanner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.stops[0].name").value("CENTRE STREET & WORTH STREET"))
                .andExpect(jsonPath("$.stops[0].longitude").value(-74.00212207));
    }

    @Test
    void determinism() throws Exception {
        String id = readyTour(p1, p2, p3);
        JsonNode a = mapper.readTree(performEvaluate(id).andReturn().getResponse().getContentAsString());
        JsonNode b = mapper.readTree(performEvaluate(id).andReturn().getResponse().getContentAsString());
        assertThat(a.get("id")).isNotEqualTo(b.get("id"));
        for (int i = 0; i < 3; i++) {
            assertThat(a.get("results").get(i).get("visitOrder")).isEqualTo(b.get("results").get(i).get("visitOrder"));
            assertThat(a.get("results").get(i).get("distanceM")).isEqualTo(b.get("results").get(i).get("distanceM"));
        }
    }

    @Test
    void unknownIds() throws Exception {
        mvc.perform(post("/api/tours/" + UUID.randomUUID() + "/evaluations").with(asPlanner()).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/evaluations/" + UUID.randomUUID()).with(asPlanner())).andExpect(status().isNotFound());
        mvc.perform(get("/api/evaluations/abc").with(asPlanner())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tours/" + UUID.randomUUID() + "/evaluations").with(asPlanner()))
                .andExpect(status().isNotFound());
    }
}
