package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.DemandEntry;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.domain.ResourceStatus;
import com.ase.wastems.domain.Tour;
import com.ase.wastems.domain.TourStatus;
import com.ase.wastems.domain.Vehicle;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.repository.VehicleRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/** Integration tests of the tour draft API (S4). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TourControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private TourRepository tours;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;
    @Autowired private VehicleRepository vehicles;
    @Autowired private CollectionPointRepository points;
    @Autowired private DepotRepository depots;
    @Autowired private WasteCategoryRepository categories;

    private Depot mn01g;
    private Depot other;
    private CollectionPoint pA;
    private CollectionPoint pB;
    private CollectionPoint pC;
    private CollectionPoint pInactive;
    private CollectionPoint pBid;
    private CollectionPoint pDsny;
    private WasteCategory paper;

    @BeforeEach
    void setUp() {
        wipe();
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
        paper = categories.save(new WasteCategory("Paper", null));
        WasteCategory glass = categories.save(new WasteCategory("Glass", null));
        mn01g =
                depots.save(
                        new Depot(
                                "MN01G",
                                -74.0104183829902,
                                40.7255225360961,
                                DataOrigin.IMPORTED_REAL,
                                "xw3j-2yxf",
                                "MN01G"));
        other = depots.save(new Depot("Other", -74.0, 40.71, DataOrigin.USER_ENTERED, null, null));
        pA = point("A St & 1st Ave", CollectionPointStatus.ACTIVE, paper, "12.50", glass, "7.50");
        pB = point("B St", CollectionPointStatus.ACTIVE, paper, "30.00", null, null);
        pC = point("C St", CollectionPointStatus.ACTIVE, paper, "5.00", null, null);
        pInactive = point("Inactive St", CollectionPointStatus.INACTIVE, paper, "10", null, null);
        pBid = point("Bid St", CollectionPointStatus.ACTIVE, paper, "10", null, null);
        pBid.setSource("8znf-7b2c", "99999", "BID-owned Compactor");
        pBid.getDemands(); // keep loaded
        pBid = points.save(pBid);
        pDsny = point("Dsny St", CollectionPointStatus.ACTIVE, paper, "10", null, null);
        pDsny.setSource("8znf-7b2c", "99998", "DSNY Wire");
        pDsny = points.save(pDsny);
    }

    @AfterEach
    void tearDown() {
        wipe();
    }

    private void wipe() {
        tours.deleteAll();
        points.deleteAll();
        depots.deleteAll();
        vehicles.deleteAll();
        categories.deleteAll();
        agents.deleteAll();
        users.deleteAll();
    }

    private CollectionPoint point(
            String name,
            CollectionPointStatus st,
            WasteCategory c1,
            String kg1,
            WasteCategory c2,
            String kg2) {
        CollectionPoint p = new CollectionPoint(name, -74.005, 40.715, DataOrigin.USER_ENTERED);
        p.setStatus(st);
        p.addDemand(new DemandEntry(c1, new BigDecimal(kg1), DataOrigin.USER_ENTERED));
        if (c2 != null) {
            p.addDemand(new DemandEntry(c2, new BigDecimal(kg2), DataOrigin.USER_ENTERED));
        }
        return points.save(p);
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

    private static String tomorrow() {
        return LocalDate.now().plusDays(1).toString();
    }

    private static String headerJson(String date, Object depotId) {
        return "{\"planningDate\":\"" + date + "\",\"depotId\":\"" + depotId + "\"}";
    }

    private ResultActions performCreate(String json) throws Exception {
        return mvc.perform(
                post("/api/tours")
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private String createDraft() throws Exception {
        MvcResult r =
                performCreate(headerJson(tomorrow(), mn01g.getId())).andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private ResultActions performAddStop(String tourId, Object pointId) throws Exception {
        return mvc.perform(
                post("/api/tours/" + tourId + "/stops")
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"collectionPointId\":\"" + pointId + "\"}"));
    }

    /** Adds the point and returns the id of the created stop. */
    private String doAddStop(String tourId, UUID pointId) throws Exception {
        MvcResult r = performAddStop(tourId, pointId).andExpect(status().isCreated()).andReturn();
        List<String> ids =
                JsonPath.read(
                        r.getResponse().getContentAsString(),
                        "$.stops[?(@.collectionPointId=='" + pointId + "')].id");
        return ids.get(0);
    }

    private ResultActions sendOrder(String tourId, String json) throws Exception {
        return mvc.perform(
                put("/api/tours/" + tourId + "/stops/order")
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions sendHeader(String tourId, String json) throws Exception {
        return mvc.perform(
                put("/api/tours/" + tourId)
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions performGet(String tourId) throws Exception {
        return mvc.perform(get("/api/tours/" + tourId).with(asPlanner()));
    }

    private static String idsJson(String... ids) {
        return "{\"stopIds\":[\"" + String.join("\",\"", ids) + "\"]}";
    }

    private static double readNumber(MvcResult r, String path) throws Exception {
        return ((Number) JsonPath.read(r.getResponse().getContentAsString(), path)).doubleValue();
    }

    @Test
    void createDraftReturns201WithEmptyDraft() throws Exception {
        String date = tomorrow();
        MvcResult r =
                performCreate(headerJson(date, mn01g.getId()))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", Matchers.matchesPattern("/api/tours/[0-9a-f-]{36}")))
                        .andExpect(jsonPath("$.status").value("DRAFT"))
                        .andExpect(jsonPath("$.origin").value("USER_ENTERED"))
                        .andExpect(jsonPath("$.revision").value(0))
                        .andExpect(jsonPath("$.planningDate").value(date))
                        .andExpect(jsonPath("$.depot.id").value(mn01g.getId().toString()))
                        .andExpect(jsonPath("$.stops").isEmpty())
                        .andExpect(jsonPath("$.collectors").isEmpty())
                        .andExpect(jsonPath("$.vehicle").doesNotExist())
                        .andExpect(jsonPath("$.driver").doesNotExist())
                        .andExpect(jsonPath("$.capacity.totalPlannedDemandKg").value(0))
                        .andExpect(jsonPath("$.capacity.vehicleCapacityKg").doesNotExist())
                        .andExpect(jsonPath("$.capacity.overCapacity").value(false))
                        .andExpect(jsonPath("$.structurallyValid").value(false))
                        .andExpect(
                                jsonPath("$.validationIssues[*].code")
                                        .value(Matchers.contains("NO_STOPS", "NO_VEHICLE", "NO_DRIVER")))
                        .andReturn();
        String id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        assertEquals("/api/tours/" + id, r.getResponse().getHeader("Location"));
    }

    @Test
    void createDraftValidation() throws Exception {
        performCreate("{\"depotId\":\"" + mn01g.getId() + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("planningDate"));
        performCreate("{\"planningDate\":\"" + tomorrow() + "\"}").andExpect(status().isBadRequest());
        performCreate(headerJson("2026-13-45", mn01g.getId())).andExpect(status().isBadRequest());
        performCreate(headerJson(LocalDate.now().minusDays(1).toString(), mn01g.getId()))
                .andExpect(status().isUnprocessableEntity());
        performCreate(headerJson(LocalDate.now().toString(), mn01g.getId()))
                .andExpect(status().isCreated());
        performCreate(headerJson(tomorrow(), UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void roleMatrix() throws Exception {
        mvc.perform(get("/api/tours").with(asAgent())).andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/tours")
                                .with(asAgent())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(headerJson(tomorrow(), mn01g.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/tours")).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/tours")
                                .with(asPlanner())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(headerJson(tomorrow(), mn01g.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/tours")
                                .with(asAdmin())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(headerJson(tomorrow(), mn01g.getId())))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/tours").with(asAdmin())).andExpect(status().isOk());
    }

    @Test
    void addStopsAppendInOrderAndSnapshotDemand() throws Exception {
        String t = createDraft();
        performAddStop(t, pA.getId())
                .andExpect(status().isCreated())
                .andExpect(
                        header().string(
                                        "Location",
                                        Matchers.matchesPattern("/api/tours/" + t + "/stops/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.stops[0].visitOrder").value(1))
                .andExpect(jsonPath("$.stops[0].status").value("PENDING"))
                .andExpect(jsonPath("$.stops[0].plannedDemands.length()").value(2))
                .andExpect(jsonPath("$.stops[0].plannedTotalKg").value(20.0))
                .andExpect(jsonPath("$.stops[0].pointStatus").value("ACTIVE"));
        performAddStop(t, pB.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stops[1].visitOrder").value(2));
        MvcResult r =
                performAddStop(t, pDsny.getId())
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.revision").value(3))
                        .andReturn();
        assertEquals(60.0, readNumber(r, "$.capacity.totalPlannedDemandKg"), 1e-9);
    }

    @Test
    void addStopRejections() throws Exception {
        String t = createDraft();
        doAddStop(t, pA.getId());
        performAddStop(t, pA.getId()).andExpect(status().isConflict());
        performAddStop(t, pInactive.getId()).andExpect(status().isUnprocessableEntity());
        performAddStop(t, pBid.getId()).andExpect(status().isUnprocessableEntity());
        performAddStop(t, UUID.randomUUID()).andExpect(status().isNotFound());
        performAddStop(t, "not-a-uuid").andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/tours/" + t + "/stops")
                                .with(asPlanner())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        performAddStop(UUID.randomUUID().toString(), pB.getId()).andExpect(status().isNotFound());
        performGet(t)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stops.length()").value(1))
                .andExpect(jsonPath("$.revision").value(1));
    }

    @Test
    void reorderPersists() throws Exception {
        String t = createDraft();
        String a = doAddStop(t, pA.getId());
        String b = doAddStop(t, pB.getId());
        String c = doAddStop(t, pC.getId());
        sendOrder(t, idsJson(c, a, b))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.stops[*].collectionPointId")
                                .value(Matchers.contains(
                                        pC.getId().toString(), pA.getId().toString(), pB.getId().toString())))
                .andExpect(jsonPath("$.stops[*].visitOrder").value(Matchers.contains(1, 2, 3)));
        performGet(t)
                .andExpect(
                        jsonPath("$.stops[*].collectionPointId")
                                .value(Matchers.contains(
                                        pC.getId().toString(), pA.getId().toString(), pB.getId().toString())))
                .andExpect(jsonPath("$.stops[*].visitOrder").value(Matchers.contains(1, 2, 3)));
    }

    @Test
    void reorderRejections() throws Exception {
        String t = createDraft();
        String a = doAddStop(t, pA.getId());
        String b = doAddStop(t, pB.getId());
        doAddStop(t, pC.getId());
        sendOrder(t, idsJson(a, b)).andExpect(status().isUnprocessableEntity());
        sendOrder(t, idsJson(a, a, b)).andExpect(status().isUnprocessableEntity());
        sendOrder(t, idsJson(a, b, UUID.randomUUID().toString())).andExpect(status().isUnprocessableEntity());
        sendOrder(t, "{}").andExpect(status().isBadRequest());
        performGet(t)
                .andExpect(
                        jsonPath("$.stops[*].collectionPointId")
                                .value(Matchers.contains(
                                        pA.getId().toString(), pB.getId().toString(), pC.getId().toString())));
    }

    @Test
    void removeStopRenumbers() throws Exception {
        String t = createDraft();
        doAddStop(t, pA.getId());
        String b = doAddStop(t, pB.getId());
        doAddStop(t, pC.getId());
        mvc.perform(delete("/api/tours/" + t + "/stops/" + b).with(asPlanner()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.stops[*].collectionPointId")
                                .value(Matchers.contains(pA.getId().toString(), pC.getId().toString())))
                .andExpect(jsonPath("$.stops[*].visitOrder").value(Matchers.contains(1, 2)));
        mvc.perform(delete("/api/tours/" + t + "/stops/" + b).with(asPlanner()).with(csrf()))
                .andExpect(status().isNotFound());
        performAddStop(t, pB.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stops[2].visitOrder").value(3));
    }

    @Test
    void pointEditsAfterSelection() throws Exception {
        String t = createDraft();
        doAddStop(t, pA.getId());
        String paperDemandId =
                points.findWithDemandsById(pA.getId()).orElseThrow().getDemands().stream()
                        .filter(d -> d.getCategory().getId().equals(paper.getId()))
                        .findFirst()
                        .orElseThrow()
                        .getId()
                        .toString();
        mvc.perform(
                        put("/api/collection-points/" + pA.getId() + "/demands/" + paperDemandId)
                                .with(asPlanner())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"categoryId\":\"" + paper.getId() + "\",\"quantityKg\":99}"))
                .andExpect(status().isOk());
        performGet(t)
                .andExpect(jsonPath("$.stops[0].plannedTotalKg").value(20.0))
                .andExpect(jsonPath("$.stops[0].plannedDemands[?(@.categoryName=='Paper')].plannedQuantityKg")
                        .value(Matchers.contains(12.5)));
        mvc.perform(
                        put("/api/collection-points/" + pA.getId())
                                .with(asPlanner())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"A St & 1st Ave\",\"longitude\":-74.0061,\"latitude\":40.715}"))
                .andExpect(status().isOk());
        performGet(t).andExpect(jsonPath("$.stops[0].longitude").value(-74.0061));
        mvc.perform(
                        patch("/api/collection-points/" + pA.getId() + "/status")
                                .with(asPlanner())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        performGet(t)
                .andExpect(jsonPath("$.stops.length()").value(1))
                .andExpect(jsonPath("$.stops[0].pointStatus").value("INACTIVE"))
                .andExpect(jsonPath("$.validationIssues[*].code").value(Matchers.hasItem("INACTIVE_STOP")))
                .andExpect(jsonPath("$.structurallyValid").value(false));
    }

    @Test
    void editHeader() throws Exception {
        String t = createDraft();
        String d2 = LocalDate.now().plusDays(2).toString();
        sendHeader(t, headerJson(d2, other.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planningDate").value(d2))
                .andExpect(jsonPath("$.depot.id").value(other.getId().toString()))
                .andExpect(jsonPath("$.revision").value(1));
        sendHeader(t, headerJson(LocalDate.now().minusDays(1).toString(), other.getId()))
                .andExpect(status().isUnprocessableEntity());
        sendHeader(t, headerJson(d2, UUID.randomUUID())).andExpect(status().isNotFound());

        LocalDate past = LocalDate.now().minusDays(3);
        String old = tours.save(new Tour(past, mn01g, DataOrigin.USER_ENTERED)).getId().toString();
        sendHeader(old, headerJson(past.toString(), other.getId())).andExpect(status().isOk());
        sendHeader(old, headerJson(LocalDate.now().minusDays(2).toString(), other.getId()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void nonDraftTourIsLocked() throws Exception {
        Tour t = new Tour(LocalDate.now().plusDays(1), mn01g, DataOrigin.USER_ENTERED);
        t.addStop(points.findWithDemandsById(pA.getId()).orElseThrow());
        t = tours.save(t);
        String id = t.getId().toString();
        String stopId = t.getStops().get(0).getId().toString();
        ReflectionTestUtils.setField(t, "status", TourStatus.PLANNED);
        tours.save(t);
        performAddStop(id, pB.getId()).andExpect(status().isConflict());
        sendOrder(id, idsJson(stopId)).andExpect(status().isConflict());
        mvc.perform(delete("/api/tours/" + id + "/stops/" + stopId).with(asPlanner()).with(csrf()))
                .andExpect(status().isConflict());
        sendHeader(id, headerJson(tomorrow(), mn01g.getId())).andExpect(status().isConflict());
    }

    @Test
    void listAndFilters() throws Exception {
        String early = createDraft();
        doAddStop(early, pA.getId());
        doAddStop(early, pB.getId());
        String laterDate = LocalDate.now().plusDays(5).toString();
        performCreate(headerJson(laterDate, mn01g.getId())).andExpect(status().isCreated());
        mvc.perform(get("/api/tours").with(asPlanner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].planningDate").value(laterDate))
                .andExpect(jsonPath("$[1].stopCount").value(2))
                .andExpect(jsonPath("$[1].totalPlannedDemandKg").value(50.0))
                .andExpect(jsonPath("$[1].depotName").value("MN01G"));
        mvc.perform(get("/api/tours?status=DRAFT").with(asPlanner()))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/tours?status=PLANNED").with(asPlanner()))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/tours?status=FOO").with(asPlanner())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tours?planningDate=" + laterDate).with(asPlanner()))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/tours?planningDate=bad").with(asPlanner())).andExpect(status().isBadRequest());
    }

    @Test
    void unknownAndMalformedIds() throws Exception {
        performGet(UUID.randomUUID().toString()).andExpect(status().isNotFound());
        performGet("not-a-uuid").andExpect(status().isBadRequest());
        String t = createDraft();
        mvc.perform(delete("/api/tours/" + t).with(asPlanner()).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }
}
