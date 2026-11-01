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

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.VehicleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Integration tests of the vehicle API. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VehicleControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;
    @Autowired private VehicleRepository vehicles;
    @Autowired private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        vehicles.deleteAll();
        agents.deleteAll();
        users.deleteAll();
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
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

    private static String json(String reg, String capacity) {
        String cap = capacity == null ? "" : ",\"capacityKg\":" + capacity;
        return "{\"registration\":\"" + reg + "\"" + cap + "}";
    }

    private ResultActions performCreate(RequestPostProcessor who, String body) throws Exception {
        return mvc.perform(
                post("/api/vehicles")
                        .with(who)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions performEdit(String id, String body) throws Exception {
        return mvc.perform(
                put("/api/vehicles/" + id)
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions sendStatus(String id, String body) throws Exception {
        return mvc.perform(
                patch("/api/vehicles/" + id + "/status")
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions fetchList(String query) throws Exception {
        return mvc.perform(get("/api/vehicles" + query).with(asPlanner()));
    }

    private String createVehicle(String reg, String capacity) throws Exception {
        String res =
                performCreate(asPlanner(), json(reg, capacity))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readTree(res).get("id").asText();
    }

    @Test
    void createNormalizesAndDefaults() throws Exception {
        performCreate(asPlanner(), json(" ny-1001 ", "9000"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.startsWith("/api/vehicles/")))
                .andExpect(jsonPath("$.registration").value("NY-1001"))
                .andExpect(jsonPath("$.capacityKg").value(9000))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.origin").value("USER_ENTERED"));
        performCreate(asAdmin(), json("NY-1002", "5000")).andExpect(status().isCreated());
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        for (String cap : new String[] {"0", "-5", null, "12.345"}) {
            performCreate(asPlanner(), json("NY-1", cap))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field=='capacityKg')]").exists());
        }
        performCreate(asPlanner(), json("  ", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='registration')]").exists());
        performCreate(asPlanner(), json("A".repeat(21), "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='registration')]").exists());
        performCreate(asPlanner(), "{not json").andExpect(status().isBadRequest());
        assertEquals(0, vehicles.count());
    }

    @Test
    void registrationIsUnique() throws Exception {
        createVehicle("NY-1001", "9000");
        performCreate(asPlanner(), json("ny-1001", "100")).andExpect(status().isConflict());
        String id = createVehicle("NY-1002", "100");
        performEdit(id, json("NY-1001", "100")).andExpect(status().isConflict());
        performEdit(id, json("ny-1002", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registration").value("NY-1002"));
    }

    @Test
    void editKeepsIdentity() throws Exception {
        String id = createVehicle("NY-1001", "9000");
        performEdit(id, json("NY-1001", "8500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.capacityKg").value(8500));
        mvc.perform(get("/api/vehicles/" + id).with(asPlanner()))
                .andExpect(jsonPath("$.capacityKg").value(8500));
        performEdit(id, json("NY-1001", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void availabilityFiltersWithoutDeleting() throws Exception {
        String id = createVehicle("NY-1001", "9000");
        sendStatus(id, "{\"status\":\"UNAVAILABLE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"));
        fetchList("?status=AVAILABLE").andExpect(jsonPath("$.length()").value(0));
        fetchList("?status=UNAVAILABLE").andExpect(jsonPath("$.length()").value(1));
        fetchList("").andExpect(jsonPath("$.length()").value(1));
        assertEquals(1, vehicles.count());
        fetchList("?status=FOO").andExpect(status().isBadRequest());
        sendStatus(id, "{\"status\":\"FOO\"}").andExpect(status().isBadRequest());
        sendStatus(id, "{\"status\":\"AVAILABLE\"}").andExpect(status().isOk());
    }

    @Test
    void errorsAreMapped() throws Exception {
        String unknown = "00000000-0000-0000-0000-000000000001";
        mvc.perform(get("/api/vehicles/" + unknown).with(asPlanner()))
                .andExpect(status().isNotFound());
        performEdit(unknown, json("X-1", "1")).andExpect(status().isNotFound());
        sendStatus(unknown, "{\"status\":\"AVAILABLE\"}").andExpect(status().isNotFound());
        mvc.perform(get("/api/vehicles/abc").with(asPlanner())).andExpect(status().isBadRequest());
        String id = createVehicle("NY-1001", "9000");
        mvc.perform(delete("/api/vehicles/" + id).with(asPlanner()).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        assertEquals(1, vehicles.count());
    }

    @Test
    void roleMatrix() throws Exception {
        String id = createVehicle("NY-1001", "9000");
        mvc.perform(get("/api/vehicles").with(asAgent())).andExpect(status().isForbidden());
        mvc.perform(get("/api/vehicles/" + id).with(asAgent())).andExpect(status().isForbidden());
        performCreate(asAgent(), json("X-1", "1")).andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/vehicles/" + id)
                                .with(asAgent())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json("X-1", "1")))
                .andExpect(status().isForbidden());
        mvc.perform(
                        patch("/api/vehicles/" + id + "/status")
                                .with(asAgent())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"UNAVAILABLE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/vehicles").with(asAdmin())).andExpect(status().isOk());
        mvc.perform(
                        post("/api/vehicles")
                                .with(asPlanner())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json("X-2", "1")))
                .andExpect(status().isForbidden());
    }
}
