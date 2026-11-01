package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** Integration tests of the operational agent API. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;
    @Autowired private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
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

    private static String json(String name) {
        return name == null ? "{}" : "{\"displayName\":\"" + name + "\"}";
    }

    private ResultActions performCreate(RequestPostProcessor who, String body) throws Exception {
        return mvc.perform(
                post("/api/agents")
                        .with(who)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions performEdit(String id, String body) throws Exception {
        return mvc.perform(
                put("/api/agents/" + id)
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions sendStatus(String id, String body) throws Exception {
        return mvc.perform(
                patch("/api/agents/" + id + "/status")
                        .with(asPlanner())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private ResultActions fetchList(String query) throws Exception {
        return mvc.perform(get("/api/agents" + query).with(asPlanner()));
    }

    private String createAgent(String name) throws Exception {
        String res =
                performCreate(asPlanner(), json(name))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readTree(res).get("id").asText();
    }

    @Test
    void createTrimsAndAllowsSameName() throws Exception {
        performCreate(asPlanner(), json("  Maria Lopez "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Maria Lopez"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.origin").value("USER_ENTERED"))
                .andExpect(jsonPath("$.linkedUser").doesNotExist());
        performCreate(asPlanner(), json("Maria Lopez")).andExpect(status().isCreated());
        assertEquals(2, agents.count());
    }

    @Test
    void invalidNamesAreRejected() throws Exception {
        for (String name : new String[] {"  ", null, "A".repeat(101)}) {
            performCreate(asPlanner(), json(name))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field=='displayName')]").exists());
        }
        assertEquals(0, agents.count());
    }

    @Test
    void editKeepsIdentity() throws Exception {
        String id = createAgent("Maria");
        performEdit(id, json("Maria L."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.displayName").value("Maria L."));
    }

    @Test
    void availabilityFilters() throws Exception {
        String id = createAgent("Maria");
        sendStatus(id, "{\"status\":\"UNAVAILABLE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNAVAILABLE"));
        fetchList("?status=AVAILABLE").andExpect(jsonPath("$.length()").value(0));
        fetchList("?status=UNAVAILABLE").andExpect(jsonPath("$.length()").value(1));
        fetchList("").andExpect(jsonPath("$.length()").value(1));
        fetchList("?status=FOO").andExpect(status().isBadRequest());
        sendStatus(id, "{\"status\":\"AVAILABLE\"}").andExpect(status().isOk());
    }

    @Test
    void errorsAreMapped() throws Exception {
        String unknown = "00000000-0000-0000-0000-000000000001";
        mvc.perform(get("/api/agents/" + unknown).with(asPlanner())).andExpect(status().isNotFound());
        performEdit(unknown, json("X")).andExpect(status().isNotFound());
        mvc.perform(get("/api/agents/abc").with(asPlanner())).andExpect(status().isBadRequest());
        String id = createAgent("Maria");
        mvc.perform(delete("/api/agents/" + id).with(asPlanner()).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        assertEquals(1, agents.count());
    }

    @Test
    void roleMatrix() throws Exception {
        String id = createAgent("Maria");
        mvc.perform(get("/api/agents").with(asAgent())).andExpect(status().isForbidden());
        mvc.perform(get("/api/agents/" + id).with(asAgent())).andExpect(status().isForbidden());
        performCreate(asAgent(), json("X")).andExpect(status().isForbidden());
        mvc.perform(get("/api/agents")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/agents").with(asAdmin())).andExpect(status().isOk());
    }
}
