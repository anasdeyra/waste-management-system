package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
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

/** Integration tests of the agent to AGENT-account link (US-16, US-04). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgentAccountLinkControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;

    private String admin;
    private String planner;
    private String agent1;
    private String agent2;
    private String maria;
    private String james;

    @BeforeEach
    void setUp() {
        agents.deleteAll();
        users.deleteAll();
        admin = users.save(new AppUser("admin1", "x", AppRole.ADMIN)).getId().toString();
        planner = users.save(new AppUser("planner1", "x", AppRole.PLANNER)).getId().toString();
        agent1 = users.save(new AppUser("agent1", "x", AppRole.AGENT)).getId().toString();
        agent2 = users.save(new AppUser("agent2", "x", AppRole.AGENT)).getId().toString();
        maria = agents.save(new Agent("Maria", DataOrigin.USER_ENTERED)).getId().toString();
        james = agents.save(new Agent("James", DataOrigin.USER_ENTERED)).getId().toString();
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

    private static String linkJson(String userId) {
        return "{\"userId\":\"" + userId + "\"}";
    }

    private ResultActions doLink(RequestPostProcessor who, String agentId, String json)
            throws Exception {
        return mvc.perform(
                put("/api/agents/" + agentId + "/user-account")
                        .with(who)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json));
    }

    private ResultActions doUnlink(RequestPostProcessor who, String agentId) throws Exception {
        return mvc.perform(delete("/api/agents/" + agentId + "/user-account").with(who).with(csrf()));
    }

    private ResultActions fetchAgent(String id) throws Exception {
        return mvc.perform(get("/api/agents/" + id).with(asAdmin()));
    }

    @Test
    void adminLinksAndRepeatIsIdempotent() throws Exception {
        doLink(asAdmin(), maria, linkJson(agent1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedUser.username").value("agent1"))
                .andExpect(jsonPath("$.linkedUser.active").value(true))
                .andExpect(content().string(Matchers.not(Matchers.containsString("passwordHash"))));
        doLink(asAdmin(), maria, linkJson(agent1)).andExpect(status().isOk());
    }

    @Test
    void linkIsOneToOne() throws Exception {
        doLink(asAdmin(), maria, linkJson(agent1)).andExpect(status().isOk());
        doLink(asAdmin(), james, linkJson(agent1)).andExpect(status().isConflict());
        doLink(asAdmin(), maria, linkJson(agent2)).andExpect(status().isConflict());
        fetchAgent(maria).andExpect(jsonPath("$.linkedUser.username").value("agent1"));
        fetchAgent(james).andExpect(jsonPath("$.linkedUser").doesNotExist());
    }

    @Test
    void onlyAgentAccountsCanBeLinked() throws Exception {
        doLink(asAdmin(), maria, linkJson(planner))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("AGENT")));
        doLink(asAdmin(), maria, linkJson(admin))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("AGENT")));
    }

    @Test
    void errorsAreMapped() throws Exception {
        String unknown = "00000000-0000-0000-0000-000000000001";
        doLink(asAdmin(), maria, linkJson(unknown)).andExpect(status().isNotFound());
        doLink(asAdmin(), unknown, linkJson(agent1)).andExpect(status().isNotFound());
        doLink(asAdmin(), maria, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='userId')]").exists());
        doLink(asAdmin(), maria, "{\"userId\":\"abc\"}").andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminMayLinkOrUnlink() throws Exception {
        doLink(asPlanner(), maria, linkJson(agent1)).andExpect(status().isForbidden());
        doUnlink(asPlanner(), maria).andExpect(status().isForbidden());
        doLink(asAgent(), maria, linkJson(agent1)).andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/agents/" + maria + "/user-account")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(linkJson(agent1)))
                .andExpect(status().is4xxClientError());
        fetchAgent(maria).andExpect(jsonPath("$.linkedUser").doesNotExist());
    }

    @Test
    void unlinkKeepsBothRecords() throws Exception {
        doLink(asAdmin(), maria, linkJson(agent1)).andExpect(status().isOk());
        doUnlink(asAdmin(), maria).andExpect(status().isNoContent());
        fetchAgent(maria).andExpect(status().isOk()).andExpect(jsonPath("$.linkedUser").doesNotExist());
        mvc.perform(get("/api/users/" + agent1).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.linkedAgentId").doesNotExist());
        assertEquals(2, agents.count());
        assertTrue(users.existsByUsername("agent1"));
        doUnlink(asAdmin(), maria).andExpect(status().isNoContent());
    }

    @Test
    void deactivatedAccountKeepsTheLink() throws Exception {
        doLink(asAdmin(), maria, linkJson(agent1)).andExpect(status().isOk());
        mvc.perform(
                        patch("/api/users/" + agent1 + "/status")
                                .with(asAdmin())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"active\":false}"))
                .andExpect(status().isOk());
        fetchAgent(maria).andExpect(jsonPath("$.linkedUser.active").value(false));
    }

    @Test
    void usersListShowsTheLinkedAgent() throws Exception {
        doLink(asAdmin(), maria, linkJson(agent1)).andExpect(status().isOk());
        mvc.perform(get("/api/users").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='agent1')].linkedAgentId").value(maria))
                .andExpect(jsonPath("$[?(@.username=='agent1')].linkedAgentName").value("Maria"))
                .andExpect(jsonPath("$[?(@.username=='planner1')].linkedAgentId").isEmpty());
    }

    private ResultActions performCreateUser(String username, String role, String agentId)
            throws Exception {
        String agentPart = agentId == null ? "" : ",\"agentId\":\"" + agentId + "\"";
        return mvc.perform(
                post("/api/users")
                        .with(asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"username\":\""
                                        + username
                                        + "\",\"password\":\"password1\",\"role\":\""
                                        + role
                                        + "\""
                                        + agentPart
                                        + "}"));
    }

    @Test
    void createWithLinkIsAtomic() throws Exception {
        performCreateUser("agent3", "AGENT", james)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.linkedAgentId").value(james));
        performCreateUser("planner9", "PLANNER", james).andExpect(status().isUnprocessableEntity());
        performCreateUser("agent4", "AGENT", james).andExpect(status().isConflict());
        performCreateUser("agent5", "AGENT", "00000000-0000-0000-0000-000000000001")
                .andExpect(status().isNotFound());
        assertFalse(users.existsByUsername("planner9"));
        assertFalse(users.existsByUsername("agent4"));
        assertFalse(users.existsByUsername("agent5"));
    }

    @Test
    void currentUserShowsAgentId() throws Exception {
        doLink(asAdmin(), maria, linkJson(agent1)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").with(asAgent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(maria));
        doUnlink(asAdmin(), maria).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").with(asAgent()))
                .andExpect(jsonPath("$.agentId").doesNotExist());
    }
}
