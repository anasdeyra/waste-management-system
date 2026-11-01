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
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.ResourceStatus;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Integration tests of the confirmation gate and the planned-tour lock (S6, US-25). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TourConfirmationControllerTest {

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
    private Agent finn;
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
        finn = agents.save(new Agent("Finn", DataOrigin.USER_ENTERED));
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
                        + "\",\"collectorIds\":[\"" + eli.getId() + "\",\"" + finn.getId() + "\"]}"))
                .andExpect(status().isOk());
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

    private ResultActions performConfirm(String tourId) throws Exception {
        return mvc.perform(post("/api/tours/" + tourId + "/confirmation").with(asPlanner()).with(csrf()));
    }

    private String acceptedTour(CollectionPoint... pts) throws Exception {
        String id = readyTour(pts);
        performAccept(id, doEvaluate(id).get("id").asText()).andExpect(status().isOk());
        return id;
    }

    private List<String> issueCodes(MvcResult r) throws Exception {
        List<String> codes = new ArrayList<>();
        mapper.readTree(r.getResponse().getContentAsString()).get("issues").forEach(i -> codes.add(i.get("code").asText()));
        return codes;
    }

    private List<String> codesOf(JsonNode node) {
        List<String> codes = new ArrayList<>();
        node.forEach(i -> codes.add(i.get("code").asText()));
        return codes;
    }

    /** Asserts a refused confirmation: 422, expected code, DRAFT, unchanged revision, not ready. */
    private void expectRefused(String id, String code, boolean alsoInConfirmationIssues) throws Exception {
        long revBefore = doGetJson("/api/tours/" + id).get("revision").asLong();
        MvcResult res = performConfirm(id).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(Matchers.startsWith("The tour cannot be confirmed (")))
                .andReturn();
        assertThat(issueCodes(res)).contains(code);
        JsonNode t = doGetJson("/api/tours/" + id);
        assertThat(t.get("status").asText()).isEqualTo("DRAFT");
        assertThat(t.get("revision").asLong()).isEqualTo(revBefore);
        assertThat(t.get("readyToConfirm").asBoolean()).isFalse();
        if (alsoInConfirmationIssues) {
            assertThat(codesOf(t.get("confirmationIssues"))).contains(code);
        }
    }

    @Test
    void newDraftReportsRouteMissing() throws Exception {
        String id = readyTour(p1, p2);
        JsonNode t = doGetJson("/api/tours/" + id);
        assertThat(codesOf(t.get("confirmationIssues"))).containsExactly("ROUTE_MISSING");
        assertThat(t.get("readyToConfirm").asBoolean()).isFalse();
        assertThat(t.has("route")).isFalse();
        MvcResult res = performConfirm(id).andExpect(status().isUnprocessableEntity()).andReturn();
        assertThat(issueCodes(res)).containsExactly("ROUTE_MISSING");
    }

    @Test
    void acceptedDraftIsReadyToConfirm() throws Exception {
        String id = acceptedTour(p1, p2);
        JsonNode t = doGetJson("/api/tours/" + id);
        assertThat(t.get("confirmationIssues")).isEmpty();
        assertThat(t.get("readyToConfirm").asBoolean()).isTrue();
    }

    @Test
    void overCapacityBlocksConfirmation() throws Exception {
        String id = acceptedTour(p1, p2);
        doAssign(id, tiny);
        assertThat(doGetJson("/api/tours/" + id).has("route")).isTrue();
        expectRefused(id, "OVER_CAPACITY", true);
    }

    @Test
    void unavailableVehicleBlocksConfirmation() throws Exception {
        String id = acceptedTour(p1, p2);
        Vehicle v = vehicles.findById(big.getId()).orElseThrow();
        v.setStatus(ResourceStatus.UNAVAILABLE);
        vehicles.save(v);
        expectRefused(id, "VEHICLE_UNAVAILABLE", true);
    }

    @Test
    void unavailableDriverAndCollectorBlockConfirmation() throws Exception {
        String id = acceptedTour(p1, p2);
        Agent d = agents.findById(dana.getId()).orElseThrow();
        d.setStatus(ResourceStatus.UNAVAILABLE);
        agents.save(d);
        expectRefused(id, "DRIVER_UNAVAILABLE", true);
        d = agents.findById(dana.getId()).orElseThrow();
        d.setStatus(ResourceStatus.AVAILABLE);
        agents.save(d);
        Agent f = agents.findById(finn.getId()).orElseThrow();
        f.setStatus(ResourceStatus.UNAVAILABLE);
        agents.save(f);
        expectRefused(id, "COLLECTOR_UNAVAILABLE", true);
    }

    @Test
    void missingCrewBlocksConfirmation() throws Exception {
        String id = acceptedTour(p1, p2);
        mvc.perform(put("/api/tours/" + id + "/assignment").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"collectorIds\":[]}"))
                .andExpect(status().isOk());
        assertThat(doGetJson("/api/tours/" + id).has("route")).isTrue();
        expectRefused(id, "NO_VEHICLE", true);
        expectRefused(id, "NO_DRIVER", true);
    }

    @Test
    void inactiveStopBlocksConfirmation() throws Exception {
        String id = acceptedTour(p1, p2);
        CollectionPoint p = points.findById(p2.getId()).orElseThrow();
        p.setStatus(CollectionPointStatus.INACTIVE);
        points.save(p);
        expectRefused(id, "INACTIVE_STOP", true);
    }

    @Test
    void movedStopMakesRouteOutdated() throws Exception {
        String id = acceptedTour(p1, p2);
        CollectionPoint p = points.findById(p1.getId()).orElseThrow();
        double lon = p.getLongitude();
        p.setLongitude(lon + 0.0005);
        points.save(p);
        expectRefused(id, "ROUTE_OUTDATED", true);
        mvc.perform(get("/api/tours/" + id).with(asPlanner())).andExpect(status().isOk());
        performConfirm(id).andExpect(jsonPath("$.message").value(Matchers.containsString("has moved")));
        p = points.findById(p1.getId()).orElseThrow();
        p.setLongitude(lon);
        points.save(p);
        performConfirm(id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void pastPlanningDateBlocksConfirmation() throws Exception {
        String id = acceptedTour(p1, p2);
        com.ase.wastems.domain.Tour t = tours.findById(UUID.fromString(id)).orElseThrow();
        ReflectionTestUtils.setField(t, "planningDate", LocalDate.now().minusDays(1));
        tours.save(t);
        performConfirm(id).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.issues[*].code").value(Matchers.hasItem("PLANNING_DATE_PAST")))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("is in the past")))
                .andExpect(jsonPath("$.message").value(Matchers.startsWith("The tour cannot be confirmed (")));
        JsonNode after = doGetJson("/api/tours/" + id);
        assertThat(after.get("status").asText()).isEqualTo("DRAFT");
        assertThat(after.get("readyToConfirm").asBoolean()).isFalse();
    }

    @Test
    void structuralEditClearsRouteAndBlocks() throws Exception {
        String id = acceptedTour(p1, p2, p3);
        List<String> ids = stopIds(doGetJson("/api/tours/" + id));
        mvc.perform(delete("/api/tours/" + id + "/stops/" + ids.get(2)).with(asPlanner()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.route").doesNotExist());
        expectRefused(id, "ROUTE_MISSING", true);
    }

    @Test
    void validDraftBecomesPlannedAndLocked() throws Exception {
        String id = acceptedTour(p1, p2, p3);
        JsonNode run = doGetJson("/api/tours/" + id + "/evaluations").get(0);
        String oldRun = run.get("id").asText();
        long revBefore = doGetJson("/api/tours/" + id).get("revision").asLong();
        performConfirm(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.revision").value(revBefore + 1))
                .andExpect(jsonPath("$.route.method").value("NN_PLUS_2_OPT"))
                .andExpect(jsonPath("$.readyToConfirm").value(false))
                .andExpect(jsonPath("$.confirmationIssues").isEmpty());
        CollectionPoint p4 = basket(categories.findAll().get(0),
                "BROADWAY & CHAMBERS STREET", -74.0066, 40.7137, "10130999");
        List<String> ids = stopIds(doGetJson("/api/tours/" + id));
        String before = tourState(id);
        mvc.perform(post("/api/tours/" + id + "/stops").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"collectionPointId\":\"" + p4.getId() + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/tours/" + id + "/stops/" + ids.get(0)).with(asPlanner()).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/tours/" + id + "/stops/order").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("stopIds", List.of(ids.get(2), ids.get(1), ids.get(0))))))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/tours/" + id + "/assignment").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"collectorIds\":[]}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/tours/" + id).with(asPlanner()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planningDate\":\"" + LocalDate.now().plusDays(3) + "\",\"depotId\":\"" + mn01g.getId() + "\"}"))
                .andExpect(status().isConflict());
        performEvaluate(id).andExpect(status().isConflict());
        performAccept(id, oldRun).andExpect(status().isConflict());
        performReject(id, oldRun).andExpect(status().isConflict());
        performConfirm(id).andExpect(status().isConflict());
        assertThat(tourState(id)).isEqualTo(before);
        JsonNode planned = doGetJson("/api/tours?status=PLANNED");
        boolean found = false;
        for (JsonNode t : planned) {
            found |= t.get("id").asText().equals(id);
        }
        assertThat(found).isTrue();
    }

    @Test
    void rejectedPathCanBeConfirmed() throws Exception {
        String id = readyTour(p1, p2);
        List<String> before = stopIds(doGetJson("/api/tours/" + id));
        performReject(id, doEvaluate(id).get("id").asText()).andExpect(status().isOk());
        performConfirm(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.route.method").value("INPUT_ORDER"));
        assertThat(stopIds(doGetJson("/api/tours/" + id))).isEqualTo(before);
    }

    @Test
    void roleMatrixAndIds() throws Exception {
        String id = acceptedTour(p1, p2);
        mvc.perform(post("/api/tours/" + id + "/confirmation").with(asAgent()).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tours/" + id + "/confirmation").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tours/" + id + "/confirmation").with(asPlanner())).andExpect(status().isForbidden());
        performConfirm(UUID.randomUUID().toString()).andExpect(status().isNotFound());
        performConfirm("not-a-uuid").andExpect(status().isBadRequest());
        mvc.perform(post("/api/tours/" + id + "/confirmation").with(asAdmin()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PLANNED"));
    }
}
