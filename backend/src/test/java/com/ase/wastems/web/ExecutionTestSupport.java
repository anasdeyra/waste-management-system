package com.ase.wastems.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Shared fixture of the S7 execution tests: three real MN01 baskets, a vehicle, four agents with
 * linked and unlinked accounts, and builders that reach every lifecycle state through the real
 * API (real GraphHopper router, like the S6 tests).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ExecutionTestSupport {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected TourRepository tours;
    @Autowired protected EvaluationRunRepository evaluationRuns;
    @Autowired protected AppUserRepository users;
    @Autowired protected AgentRepository agents;
    @Autowired protected VehicleRepository vehicles;
    @Autowired protected CollectionPointRepository points;
    @Autowired protected DepotRepository depots;
    @Autowired protected WasteCategoryRepository categories;
    @Autowired protected SourceSnapshotRepository sourceSnapshots;

    protected WasteCategory paper;
    protected WasteCategory glass;
    protected WasteCategory oldCat;
    protected Depot mn01g;
    protected CollectionPoint p1;
    protected CollectionPoint p2;
    protected CollectionPoint p3;
    protected Vehicle big;
    protected Agent dana;
    protected Agent eli;
    protected Agent finn;
    protected Agent gus;

    @BeforeEach
    void setUpExecution() {
        wipe();
        paper = categories.save(new WasteCategory("Paper", null));
        glass = categories.save(new WasteCategory("Glass", null));
        oldCat = categories.save(new WasteCategory("Old category", null));
        oldCat.setActive(false);
        oldCat = categories.save(oldCat);
        mn01g = depots.save(new Depot("MN01G", -74.0104183829902, 40.7255225360961, DataOrigin.IMPORTED_REAL,
                "xw3j-2yxf", "MN01G"));
        p1 = basket("CENTRE STREET & WORTH STREET", -74.00212207, 40.71528035, "10130067", paper, "10.00", glass, "5.00");
        p2 = basket("BROADWAY & LEONARD STREET", -74.0040862, 40.71705309, "10130182", paper, "10.00", null, null);
        p3 = basket("BROADWAY & THOMAS STREET", -74.00523497, 40.71570284, "10130187", paper, "10.00", null, null);
        big = vehicles.save(new Vehicle("EV-BIG", new BigDecimal("1000.00"), DataOrigin.USER_ENTERED));
        dana = agents.save(new Agent("Dana", DataOrigin.USER_ENTERED));
        eli = agents.save(new Agent("Eli", DataOrigin.USER_ENTERED));
        finn = agents.save(new Agent("Finn", DataOrigin.USER_ENTERED));
        gus = agents.save(new Agent("Gus", DataOrigin.USER_ENTERED));
        AppUser a1 = users.save(new AppUser("agent1", "x", AppRole.AGENT));
        AppUser a2 = users.save(new AppUser("agent2", "x", AppRole.AGENT));
        AppUser a3 = users.save(new AppUser("agent3", "x", AppRole.AGENT));
        AppUser a4 = users.save(new AppUser("agent4", "x", AppRole.AGENT));
        users.save(new AppUser("agent5", "x", AppRole.AGENT));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        link(dana, a1);
        link(eli, a2);
        link(finn, a3);
        link(gus, a4);
        sourceSnapshots.findBySourceDataset("8znf-7b2c")
                .orElseGet(() -> sourceSnapshots.save(new SourceSnapshot("8znf-7b2c", "data/nyc-mn01-baskets.csv",
                        449, "a0737237e370".repeat(5) + "abcd", "8znf-7b2c:449:a0737237e370", Instant.now())));
    }

    private void link(Agent agent, AppUser account) {
        agent.linkUserAccount(account);
        agents.save(agent);
    }

    private CollectionPoint basket(String name, double lon, double lat, String id, WasteCategory c1, String kg1,
            WasteCategory c2, String kg2) {
        CollectionPoint p = new CollectionPoint(name, lon, lat, DataOrigin.IMPORTED_REAL);
        p.setSource("8znf-7b2c", id, "DSNY PSR");
        p.addDemand(new DemandEntry(c1, new BigDecimal(kg1), DataOrigin.USER_ENTERED));
        if (c2 != null) {
            p.addDemand(new DemandEntry(c2, new BigDecimal(kg2), DataOrigin.USER_ENTERED));
        }
        return points.save(p);
    }

    @AfterEach
    void tearDownExecution() {
        wipe();
    }

    protected void wipe() {
        evaluationRuns.deleteAll();
        tours.deleteAll();
        points.deleteAll();
        depots.deleteAll();
        vehicles.deleteAll();
        agents.deleteAll();
        categories.deleteAll();
        users.deleteAll();
    }

    protected static RequestPostProcessor asAgent(String username) {
        return user(username).roles("AGENT");
    }

    protected static RequestPostProcessor asPlanner() {
        return user("planner1").roles("PLANNER");
    }

    protected static RequestPostProcessor asAdmin() {
        return user("admin1").roles("ADMIN");
    }

    protected ResultActions performStart(UUID tourId, String username) throws Exception {
        return mvc.perform(post("/api/tours/" + tourId + "/start").with(asAgent(username)).with(csrf()));
    }

    protected ResultActions performOutcome(UUID tourId, UUID stopId, String username, String json) throws Exception {
        return mvc.perform(post("/api/tours/" + tourId + "/stops/" + stopId + "/outcome")
                .with(asAgent(username)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions performComplete(UUID tourId, String username) throws Exception {
        return mvc.perform(post("/api/tours/" + tourId + "/completion").with(asAgent(username)).with(csrf()));
    }

    protected ResultActions performMine(String username) throws Exception {
        return mvc.perform(get("/api/tours/mine").with(asAgent(username)));
    }

    protected ResultActions performMineOne(UUID tourId, String username) throws Exception {
        return mvc.perform(get("/api/tours/mine/" + tourId).with(asAgent(username)));
    }

    protected UUID doCreateDraft() throws Exception {
        return doCreateDraft(LocalDate.now().plusDays(1));
    }

    protected UUID doCreateDraft(LocalDate date) throws Exception {
        MvcResult r = mvc.perform(post("/api/tours").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planningDate\":\"" + date + "\",\"depotId\":\"" + mn01g.getId() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        UUID id = UUID.fromString(JsonPath.read(r.getResponse().getContentAsString(), "$.id"));
        for (CollectionPoint p : List.of(p1, p2, p3)) {
            mvc.perform(post("/api/tours/" + id + "/stops").with(asPlanner()).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"collectionPointId\":\"" + p.getId() + "\"}")).andExpect(status().isCreated());
        }
        mvc.perform(put("/api/tours/" + id + "/assignment").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehicleId\":\"" + big.getId() + "\",\"driverId\":\"" + dana.getId()
                        + "\",\"collectorIds\":[\"" + eli.getId() + "\",\"" + finn.getId() + "\"]}"))
                .andExpect(status().isOk());
        return id;
    }

    protected UUID doCreatePlanned() throws Exception {
        return doCreatePlanned(LocalDate.now().plusDays(1));
    }

    protected UUID doCreatePlanned(LocalDate date) throws Exception {
        UUID id = doCreateDraft(date);
        MvcResult run = mvc.perform(post("/api/tours/" + id + "/evaluations").with(asPlanner()).with(csrf()))
                .andExpect(status().isCreated()).andReturn();
        String runId = JsonPath.read(run.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/api/tours/" + id + "/proposal-acceptances").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"evaluationRunId\":\"" + runId + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/tours/" + id + "/confirmation").with(asPlanner()).with(csrf()))
                .andExpect(status().isOk());
        return id;
    }

    protected UUID doCreateInProgress() throws Exception {
        UUID id = doCreatePlanned();
        performStart(id, "agent1").andExpect(status().isOk());
        return id;
    }

    protected JsonNode readTour(UUID id) throws Exception {
        return mapper.readTree(mvc.perform(get("/api/tours/" + id).with(asPlanner()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    protected List<UUID> stopIds(UUID tourId) throws Exception {
        List<UUID> ids = new ArrayList<>();
        readTour(tourId).get("stops").forEach(s -> ids.add(UUID.fromString(s.get("id").asText())));
        return ids;
    }

    /** The stop of the given point, from a tour JSON. */
    protected UUID stopOfPoint(UUID tourId, CollectionPoint p) throws Exception {
        for (JsonNode s : readTour(tourId).get("stops")) {
            if (s.get("collectionPointId").asText().equals(p.getId().toString())) {
                return UUID.fromString(s.get("id").asText());
            }
        }
        throw new AssertionError("no stop for point " + p.getName());
    }

}
