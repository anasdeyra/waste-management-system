package com.ase.wastems.web;

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

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.WasteCategory;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Integration tests of the collection point and demand API. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CollectionPointControllerTest {

    private static final String LON = "-74.00212207";
    private static final String LAT = "40.71528035";

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;
    @Autowired private WasteCategoryRepository categories;
    @Autowired private CollectionPointRepository points;
    @Autowired private DepotRepository depots;
    @Autowired private ObjectMapper mapper;

    private String paperId;
    private String glassId;
    private String oldId;

    @BeforeEach
    void setUp() {
        points.deleteAll();
        depots.deleteAll();
        categories.deleteAll();
        agents.deleteAll();
        users.deleteAll();
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
        paperId = categories.save(new WasteCategory("Paper", null)).getId().toString();
        glassId = categories.save(new WasteCategory("Glass", null)).getId().toString();
        WasteCategory old = new WasteCategory("Old", null);
        old.setActive(false);
        oldId = categories.save(old).getId().toString();
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

    private static String pointJson(String name, String lon, String lat, String demands) {
        return "{\"name\":\"" + name + "\",\"longitude\":" + lon + ",\"latitude\":" + lat
                + ",\"demands\":" + demands + "}";
    }

    private static String demand(String categoryId, String kg) {
        return "{\"categoryId\":\"" + categoryId + "\",\"quantityKg\":" + kg + "}";
    }

    private String paper25() {
        return "[" + demand(paperId, "25") + "]";
    }

    private ResultActions performCreate(RequestPostProcessor who, String json) throws Exception {
        return mvc.perform(
                post("/api/collection-points")
                        .with(who)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions performUpdate(String id, String json) throws Exception {
        return mvc.perform(
                put("/api/collection-points/" + id)
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions sendStatus(String id, String json) throws Exception {
        return mvc.perform(
                patch("/api/collection-points/" + id + "/status")
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions performAddDemand(String id, String json) throws Exception {
        return mvc.perform(
                post("/api/collection-points/" + id + "/demands")
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions performEditDemand(String id, String demandId, String json)
            throws Exception {
        return mvc.perform(
                put("/api/collection-points/" + id + "/demands/" + demandId)
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions performRemoveDemand(String id, String demandId) throws Exception {
        return mvc.perform(
                delete("/api/collection-points/" + id + "/demands/" + demandId)
                        .with(asPlanner())
                        .with(csrf()));
    }

    private ResultActions fetchList(String query) throws Exception {
        return mvc.perform(get("/api/collection-points" + query).with(asPlanner()));
    }

    private ResultActions fetchOne(String id) throws Exception {
        return mvc.perform(get("/api/collection-points/" + id).with(asPlanner()));
    }

    private String createPoint(String name, String demands) throws Exception {
        String res =
                performCreate(asPlanner(), pointJson(name, LON, LAT, demands))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readTree(res).get("id").asText();
    }

    private String firstDemandId(String pointId) throws Exception {
        String res =
                fetchOne(pointId).andReturn().getResponse().getContentAsString();
        return mapper.readTree(res).get("demands").get(0).get("id").asText();
    }

    private String demandIdOf(String pointId, String categoryName) throws Exception {
        String res = fetchOne(pointId).andReturn().getResponse().getContentAsString();
        for (var d : mapper.readTree(res).get("demands")) {
            if (d.get("categoryName").asText().equals(categoryName)) {
                return d.get("id").asText();
            }
        }
        throw new AssertionError("no demand for " + categoryName);
    }

    @Test
    void plannerCreatesPoint() throws Exception {
        performCreate(asPlanner(), pointJson("A", LON, LAT, "[" + demand(paperId, "25") + "]"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.startsWith("/api/collection-points/")))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.origin").value("USER_ENTERED"))
                .andExpect(jsonPath("$.longitude").value(-74.00212207))
                .andExpect(jsonPath("$.latitude").value(40.71528035))
                .andExpect(jsonPath("$.demands.length()").value(1))
                .andExpect(jsonPath("$.demands[0].categoryName").value("Paper"))
                .andExpect(jsonPath("$.demands[0].quantityKg").value(25.0))
                .andExpect(jsonPath("$.demands[0].origin").value("USER_ENTERED"))
                .andExpect(jsonPath("$.totalDemandKg").value(25.0))
                .andExpect(jsonPath("$.sourceDataset").doesNotExist());
        performCreate(asAdmin(), pointJson("B", LON, LAT, paper25()))
                .andExpect(status().isCreated());
    }

    @Test
    void roleMatrix() throws Exception {
        String id = createPoint("A", paper25());
        String d = firstDemandId(id);
        String body = pointJson("X", LON, LAT, paper25());
        mvc.perform(get("/api/collection-points").with(asAgent())).andExpect(status().isForbidden());
        mvc.perform(get("/api/collection-points/" + id).with(asAgent()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/collection-points").with(asAgent()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/collection-points/" + id).with(asAgent()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/collection-points/" + id + "/status").with(asAgent()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/collection-points/" + id + "/demands").with(asAgent()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(demand(glassId, "5")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/collection-points/" + id + "/demands/" + d).with(asAgent()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(demand(paperId, "5")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/collection-points/" + id + "/demands/" + d).with(asAgent()).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/depots").with(asAgent())).andExpect(status().isForbidden());
        mvc.perform(get("/api/collection-points")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/collection-points").with(asPlanner())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordinateValidation() throws Exception {
        performCreate(asPlanner(), pointJson("A", LON, "95", paper25()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='latitude')]").exists());
        performCreate(asPlanner(), pointJson("A", "-200", LAT, paper25()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='longitude')]").exists());
        performCreate(asPlanner(), "{\"name\":\"A\",\"latitude\":" + LAT + ",\"demands\":" + paper25() + "}")
                .andExpect(status().isBadRequest());
        performCreate(asPlanner(), pointJson("A", "40.71528035", "-74.00212207", paper25()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("swapped")));
        performCreate(asPlanner(), pointJson("A", "2.35", "48.85", paper25()))
                .andExpect(status().isUnprocessableEntity());
        fetchList("").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void demandValidationOnCreate() throws Exception {
        performCreate(asPlanner(), pointJson("A", LON, LAT, "[" + demand(paperId, "0") + "]"))
                .andExpect(status().isBadRequest());
        performCreate(asPlanner(), pointJson("A", LON, LAT, "[" + demand(paperId, "-5") + "]"))
                .andExpect(status().isBadRequest());
        performCreate(asPlanner(), pointJson("A", LON, LAT, "[" + demand(paperId, "12.345") + "]"))
                .andExpect(status().isBadRequest());
        performCreate(asPlanner(), pointJson("A", LON, LAT, "[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='demands')]").exists());
        performCreate(asPlanner(), pointJson("A", LON, LAT, "[{\"quantityKg\":5}]"))
                .andExpect(status().isBadRequest());
        performCreate(asPlanner(), pointJson("A", LON, LAT, "[" + demand(oldId, "5") + "]"))
                .andExpect(status().isUnprocessableEntity());
        performCreate(
                        asPlanner(),
                        pointJson("A", LON, LAT, "[" + demand(UUID.randomUUID().toString(), "5") + "]"))
                .andExpect(status().isNotFound());
        performCreate(
                        asPlanner(),
                        pointJson("A", LON, LAT, "[" + demand(paperId, "5") + "," + demand(paperId, "6") + "]"))
                .andExpect(status().isConflict());
        performCreate(asPlanner(), pointJson("  ", LON, LAT, paper25()))
                .andExpect(status().isBadRequest());
        fetchList("").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void updateChangesNameAndCoordinates() throws Exception {
        String id = createPoint("A", paper25());
        performUpdate(id, "{\"name\":\"A2\",\"longitude\":-74.0060,\"latitude\":40.7160}")
                .andExpect(status().isOk());
        fetchOne(id)
                .andExpect(jsonPath("$.name").value("A2"))
                .andExpect(jsonPath("$.longitude").value(-74.006))
                .andExpect(jsonPath("$.latitude").value(40.716));
        performUpdate(id, "{\"name\":\"A3\",\"longitude\":-74.0060,\"latitude\":95}")
                .andExpect(status().isBadRequest());
        fetchOne(id)
                .andExpect(jsonPath("$.name").value("A2"))
                .andExpect(jsonPath("$.latitude").value(40.716));
        performUpdate(id, "{\"name\":\"A3\",\"longitude\":40.716,\"latitude\":-74.006}")
                .andExpect(status().isUnprocessableEntity());
        performUpdate(
                        UUID.randomUUID().toString(),
                        "{\"name\":\"A3\",\"longitude\":-74.006,\"latitude\":40.716}")
                .andExpect(status().isNotFound());
        performUpdate("abc", "{\"name\":\"A3\",\"longitude\":-74.006,\"latitude\":40.716}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusChangeAndFilters() throws Exception {
        String a = createPoint("A", "[" + demand(paperId, "5") + "]");
        createPoint("B", "[" + demand(glassId, "5") + "]");
        String c = createPoint("C", "[" + demand(paperId, "5") + "," + demand(glassId, "6") + "]");
        sendStatus(c, "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        fetchList("?status=ACTIVE")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[1].name").value("B"));
        fetchList("?status=INACTIVE")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("C"));
        fetchList("").andExpect(jsonPath("$.length()").value(3));
        fetchList("?categoryId=" + paperId)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("A"))
                .andExpect(jsonPath("$[1].name").value("C"));
        fetchList("?status=ACTIVE&categoryId=" + glassId)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("B"));
        fetchList("?status=FOO").andExpect(status().isBadRequest());
        sendStatus(c, "{\"status\":\"FOO\"}").andExpect(status().isBadRequest());
        sendStatus(c, "{\"status\":\"ACTIVE\"}").andExpect(status().isOk());
        sendStatus(a, "{\"status\":\"INACTIVE\"}").andExpect(status().isOk());
    }

    @Test
    void demandEntriesLifecycle() throws Exception {
        String a = createPoint("A", paper25());
        String other = createPoint("Other", "[" + demand(paperId, "1") + "]");
        String otherDemand = firstDemandId(other);

        String res =
                performAddDemand(a, demand(glassId, "10"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.categoryName").value("Glass"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String glassDemandId = mapper.readTree(res).get("id").asText();
        mvc.perform(
                        post("/api/collection-points/" + a + "/demands")
                                .with(asPlanner())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(demand(paperId, "1")))
                .andExpect(status().isConflict());
        fetchOne(a)
                .andExpect(jsonPath("$.demands.length()").value(2))
                .andExpect(jsonPath("$.totalDemandKg").value(35.0));
        performAddDemand(a, demand(paperId, "3")).andExpect(status().isConflict());
        performAddDemand(a, demand(oldId, "3")).andExpect(status().isUnprocessableEntity());
        performAddDemand(a, demand(glassId, "0")).andExpect(status().isBadRequest());

        performEditDemand(a, glassDemandId, demand(glassId, "12.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityKg").value(12.5));
        performEditDemand(a, glassDemandId, demand(paperId, "12.5")).andExpect(status().isConflict());
        performRemoveDemand(a, glassDemandId).andExpect(status().isNoContent());
        String paperDemandId = firstDemandId(a);
        performRemoveDemand(a, paperDemandId).andExpect(status().isUnprocessableEntity());
        fetchOne(a).andExpect(jsonPath("$.demands.length()").value(1));
        performRemoveDemand(a, otherDemand).andExpect(status().isNotFound());
    }

    @Test
    void inactiveCategoryIsKeptOnEdit() throws Exception {
        String a = createPoint("A", paper25());
        String d = firstDemandId(a);
        mvc.perform(
                        patch("/api/waste-categories/" + paperId + "/status")
                                .with(asAdmin())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"active\":false}"))
                .andExpect(status().isOk());
        performEditDemand(a, d, demand(paperId, "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityKg").value(30.0));
        fetchOne(a).andExpect(jsonPath("$.demands[0].categoryActive").value(false));
        // an edit that moves the demand to another (new) inactive category is refused
        performEditDemand(a, d, demand(oldId, "30")).andExpect(status().isUnprocessableEntity());
        // sanity: demandIdOf helper resolves by name
        org.junit.jupiter.api.Assertions.assertEquals(d, demandIdOf(a, "Paper"));
    }

    @Test
    void eligibilityIsExposedAndFilterable() throws Exception {
        String userPoint = createPoint("User point", paper25());
        String inactive = createPoint("Inactive point", paper25());
        mvc.perform(get("/api/collection-points/" + userPoint).with(asPlanner()))
                .andExpect(jsonPath("$.optimizationEligible").value(true));
        sendStatus(inactive, "{\"status\":\"INACTIVE\"}")
                .andExpect(jsonPath("$.optimizationEligible").value(false));

        com.ase.wastems.domain.CollectionPoint bid =
                new com.ase.wastems.domain.CollectionPoint(
                        "BID point", -74.0, 40.71, com.ase.wastems.domain.DataOrigin.IMPORTED_REAL);
        bid.setSource("8znf-7b2c", "X1", "BID-owned Compactor");
        bid.addDemand(
                new com.ase.wastems.domain.DemandEntry(
                        categories.findById(java.util.UUID.fromString(paperId)).orElseThrow(),
                        java.math.BigDecimal.ONE,
                        com.ase.wastems.domain.DataOrigin.SYNTHETIC));
        String bidId = points.save(bid).getId().toString();
        fetchOne(bidId).andExpect(jsonPath("$.optimizationEligible").value(false));

        fetchList("?eligible=true")
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("User point"));
        fetchList("?eligible=false")
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name=='Inactive point')]").exists())
                .andExpect(jsonPath("$[?(@.name=='BID point')]").exists());
        fetchList("?eligible=maybe").andExpect(status().isBadRequest());
    }
}
