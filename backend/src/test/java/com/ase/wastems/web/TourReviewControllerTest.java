package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/** Integration tests of accepting or keeping the input order of an evaluated proposal (S6, US-24). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TourReviewControllerTest {

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

    private ResultActions performAccept(String tourId, String runId) throws Exception {
        return mvc.perform(post("/api/tours/" + tourId + "/proposal-acceptances").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationRunId\":\"" + runId + "\"}"));
    }

    private ResultActions performReject(String tourId, String runId) throws Exception {
        return mvc.perform(post("/api/tours/" + tourId + "/proposal-rejections").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationRunId\":\"" + runId + "\"}"));
    }

    private JsonNode doEvaluate(String tourId) throws Exception {
        return mapper.readTree(performEvaluate(tourId).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString());
    }

    private JsonNode doGetJson(String path) throws Exception {
        return mapper.readTree(doGetBody(path));
    }

    private JsonNode result(JsonNode run, String method) {
        for (JsonNode r : run.get("results")) {
            if (r.get("method").asText().equals(method)) {
                return r;
            }
        }
        throw new AssertionError("no result " + method);
    }

    private List<String> stopPointIds(JsonNode tour) {
        List<String> ids = new ArrayList<>();
        tour.get("stops").forEach(s -> ids.add(s.get("collectionPointId").asText()));
        return ids;
    }

    private List<String> stopIds(JsonNode tour) {
        List<String> ids = new ArrayList<>();
        tour.get("stops").forEach(s -> ids.add(s.get("id").asText()));
        return ids;
    }

    private void doReorder(String tourId, List<String> stopIds) throws Exception {
        mvc.perform(put("/api/tours/" + tourId + "/stops/order").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("stopIds", stopIds))))
                .andExpect(status().isOk());
    }

    @Test
    void acceptAppliesProposalOrderAndRoute() throws Exception {
        String id = readyTour(p3, p1, p2);
        JsonNode run = doEvaluate(id);
        String runId = run.get("id").asText();
        long rev = run.get("tourRevision").asLong();
        JsonNode proposal = result(run, "NN_PLUS_2_OPT");
        MvcResult res = performAccept(id, runId).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.revision").value(rev + 1))
                .andExpect(jsonPath("$.route.method").value("NN_PLUS_2_OPT"))
                .andExpect(jsonPath("$.route.evaluationRunId").value(runId))
                .andExpect(jsonPath("$.route.roadGraphVersion").value("osm-mn01-e1f2fabaa937"))
                .andExpect(jsonPath("$.route.distanceM").value(proposal.get("distanceM").asDouble()))
                .andExpect(jsonPath("$.route.durationS").value(proposal.get("durationS").asDouble()))
                .andExpect(jsonPath("$.route.legs.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.route.legs.features.length()").value(4))
                .andReturn();
        JsonNode tour = mapper.readTree(res.getResponse().getContentAsString());
        List<String> expected = new ArrayList<>();
        proposal.get("collectionPointOrder").forEach(n -> expected.add(n.asText()));
        assertThat(stopPointIds(tour)).isEqualTo(expected);
        for (int i = 0; i < 3; i++) {
            assertThat(tour.get("stops").get(i).get("visitOrder").asInt()).isEqualTo(i + 1);
        }
        mvc.perform(get("/api/evaluations/" + runId).with(asPlanner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedBy").value("planner1"))
                .andExpect(jsonPath("$.decidedAt").exists())
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.results[2].visitOrder").value(
                        Matchers.contains(proposal.get("visitOrder").get(0).asInt(),
                                proposal.get("visitOrder").get(1).asInt(),
                                proposal.get("visitOrder").get(2).asInt())));
        mvc.perform(get("/api/tours/" + id + "/evaluations").with(asPlanner()))
                .andExpect(jsonPath("$[0].decision").value("ACCEPTED"));
        JsonNode list = doGetJson("/api/tours?status=DRAFT");
        boolean found = false;
        for (JsonNode t : list) {
            if (t.get("id").asText().equals(id)) {
                assertThat(t.get("routeMethod").asText()).isEqualTo("NN_PLUS_2_OPT");
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void rejectKeepsOrderAndAttachesInputOrderRoute() throws Exception {
        String id = readyTour(p3, p1, p2);
        JsonNode run = doEvaluate(id);
        List<String> before = stopIds(doGetJson("/api/tours/" + id));
        long rev = run.get("tourRevision").asLong();
        MvcResult res = performReject(id, run.get("id").asText()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.revision").value(rev + 1))
                .andExpect(jsonPath("$.route.method").value("INPUT_ORDER"))
                .andExpect(jsonPath("$.route.distanceM").value(result(run, "INPUT_ORDER").get("distanceM").asDouble()))
                .andReturn();
        assertThat(stopIds(mapper.readTree(res.getResponse().getContentAsString()))).isEqualTo(before);
        JsonNode after = doGetJson("/api/evaluations/" + run.get("id").asText());
        assertThat(after.get("decision").asText()).isEqualTo("REJECTED");
        assertThat(after.get("results")).hasSize(3);
        JsonNode nn2 = result(after, "NN_PLUS_2_OPT");
        assertThat(nn2.get("feasible").asBoolean()).isTrue();
        assertThat(nn2.get("legs").get("features")).hasSize(4);
    }

    @Test
    void staleProposalIsRefusedAndChangesNothing() throws Exception {
        String id = readyTour(p1, p2, p3);
        JsonNode run = doEvaluate(id);
        String runId = run.get("id").asText();
        List<String> ids = stopIds(doGetJson("/api/tours/" + id));
        doReorder(id, List.of(ids.get(2), ids.get(0), ids.get(1)));
        String before = tourState(id);
        performAccept(id, runId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("changed since this evaluation")));
        performReject(id, runId).andExpect(status().isConflict());
        assertThat(tourState(id)).isEqualTo(before);
        mvc.perform(get("/api/tours/" + id).with(asPlanner())).andExpect(jsonPath("$.route").doesNotExist());
        mvc.perform(get("/api/evaluations/" + runId).with(asPlanner())).andExpect(jsonPath("$.decision").doesNotExist());

        JsonNode r2 = doEvaluate(id);
        List<String> ids2 = stopIds(doGetJson("/api/tours/" + id));
        mvc.perform(delete("/api/tours/" + id + "/stops/" + ids2.get(2)).with(asPlanner()).with(csrf()))
                .andExpect(status().isOk());
        performAccept(id, r2.get("id").asText()).andExpect(status().isConflict());
        JsonNode tour = doGetJson("/api/tours/" + id);
        assertThat(tour.get("stops")).hasSize(2);
        assertThat(tour.has("route")).isFalse();
    }

    @Test
    void secondAcceptIsRefused() throws Exception {
        String id = readyTour(p1, p2, p3);
        String runId = doEvaluate(id).get("id").asText();
        performAccept(id, runId).andExpect(status().isOk());
        performAccept(id, runId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("already accepted")));
        performReject(id, runId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("already accepted")));
    }

    @Test
    void routingFailedRunHasNoProposal() throws Exception {
        String id = readyTour(p1, queens);
        JsonNode run = mapper.readTree(performEvaluate(id).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ROUTING_FAILED")).andReturn().getResponse().getContentAsString());
        String before = tourState(id);
        performAccept(id, run.get("id").asText()).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("has no proposal")));
        performReject(id, run.get("id").asText()).andExpect(status().isUnprocessableEntity());
        assertThat(tourState(id)).isEqualTo(before);
        assertThat(doGetJson("/api/tours/" + id).has("route")).isFalse();
    }

    @Test
    void runOfAnotherTourIsRefused() throws Exception {
        String a = readyTour(p1, p2);
        String b = readyTour(p2, p3);
        String runB = doEvaluate(b).get("id").asText();
        String before = tourState(a);
        performAccept(a, runB).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("belongs to another tour")));
        assertThat(tourState(a)).isEqualTo(before);
    }

    @Test
    void idsAndBodies() throws Exception {
        String id = readyTour(p1, p2);
        String runId = doEvaluate(id).get("id").asText();
        performAccept(UUID.randomUUID().toString(), runId).andExpect(status().isNotFound());
        performAccept(id, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        mvc.perform(post("/api/tours/" + id + "/proposal-acceptances").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/tours/" + id + "/proposal-rejections").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationRunId\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tours/not-a-uuid/proposal-acceptances").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationRunId\":\"" + runId + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/evaluations/" + runId).with(asPlanner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").doesNotExist());
    }

    @Test
    void roleMatrix() throws Exception {
        String id = readyTour(p1, p2, p3);
        String runId = doEvaluate(id).get("id").asText();
        String body = "{\"evaluationRunId\":\"" + runId + "\"}";
        for (String path : List.of("proposal-acceptances", "proposal-rejections")) {
            mvc.perform(post("/api/tours/" + id + "/" + path).with(asAgent()).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
            mvc.perform(post("/api/tours/" + id + "/" + path).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/tours/" + id + "/" + path).with(asPlanner())
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/tours/" + id + "/proposal-acceptances").with(asAdmin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mvc.perform(get("/api/evaluations/" + runId).with(asPlanner()))
                .andExpect(jsonPath("$.decidedBy").value("admin1"));
    }

    @Test
    void planningDateChangeKeepsRouteButStopChangeClearsIt() throws Exception {
        String id = readyTour(p1, p2);
        performAccept(id, doEvaluate(id).get("id").asText()).andExpect(status().isOk());
        mvc.perform(put("/api/tours/" + id).with(asPlanner()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planningDate\":\"" + LocalDate.now().plusDays(2) + "\",\"depotId\":\"" + mn01g.getId() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.route").exists());
        doAssign(id, big);
        mvc.perform(get("/api/tours/" + id).with(asPlanner())).andExpect(jsonPath("$.route").exists());
        doAddStop(id, p3);
        mvc.perform(get("/api/tours/" + id).with(asPlanner())).andExpect(jsonPath("$.route").doesNotExist());

        String id2 = readyTour(p1, p2);
        performAccept(id2, doEvaluate(id2).get("id").asText()).andExpect(status().isOk());
        Depot other = depots.save(new Depot("Other", -74.0, 40.72, DataOrigin.USER_ENTERED, null, null));
        mvc.perform(put("/api/tours/" + id2).with(asPlanner()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planningDate\":\"" + LocalDate.now().plusDays(1) + "\",\"depotId\":\"" + other.getId() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.route").doesNotExist());
    }
}
