package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Integration tests of the user administration API. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;
    @Autowired private PasswordEncoder encoder;
    @Autowired private ObjectMapper mapper;

    private UUID adminId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        agents.deleteAll();
        users.deleteAll();
        adminId = users.save(new AppUser("admin1", encoder.encode("secret123"), AppRole.ADMIN)).getId();
        users.save(new AppUser("planner1", encoder.encode("secret123"), AppRole.PLANNER));
        agentId = users.save(new AppUser("agent1", encoder.encode("secret123"), AppRole.AGENT)).getId();
    }

    private static RequestPostProcessor admin() {
        return user("admin1").roles("ADMIN");
    }

    private RequestBuilder operation(int index, RequestPostProcessor who) {
        Function<RequestPostProcessor, RequestBuilder> op =
                switch (index) {
                    case 0 -> w -> get("/api/users").with(w).with(csrf());
                    case 1 -> w -> get("/api/users/" + adminId).with(w).with(csrf());
                    case 2 ->
                            w ->
                                    post("/api/users")
                                            .with(w)
                                            .with(csrf())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    "{\"username\":\"new-"
                                                            + UUID.randomUUID().toString().substring(0, 8)
                                                            + "\",\"password\":\"password1\",\"role\":\"AGENT\"}");
                    default ->
                            w ->
                                    patch("/api/users/" + agentId + "/status")
                                            .with(w)
                                            .with(csrf())
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content("{\"active\":true}");
                };
        return op.apply(who);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void roleMatrix(int op) throws Exception {
        mvc.perform(operation(op, user("planner1").roles("PLANNER"))).andExpect(status().isForbidden());
        mvc.perform(operation(op, user("agent1").roles("AGENT"))).andExpect(status().isForbidden());
        mvc.perform(operation(op, request -> request)).andExpect(status().isUnauthorized());
        int code = mvc.perform(operation(op, admin())).andReturn().getResponse().getStatus();
        assertTrue(code == 200 || code == 201, "admin got " + code);
    }

    private org.springframework.test.web.servlet.ResultActions create(String body) throws Exception {
        return mvc.perform(
                post("/api/users")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions login(String u, String p)
            throws Exception {
        return mvc.perform(
                post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + u + "\",\"password\":\"" + p + "\"}"));
    }

    @Test
    void createUserReturns201WithoutPassword() throws Exception {
        String res =
                create("{\"username\":\"Carol\",\"password\":\"password1\",\"role\":\"PLANNER\"}")
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.username").value("carol"))
                        .andExpect(jsonPath("$.active").value(true))
                        .andExpect(jsonPath("$.password").doesNotExist())
                        .andExpect(jsonPath("$.passwordHash").doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        JsonNode json = mapper.readTree(res);
        String id = json.get("id").asText();
        mvc.perform(get("/api/users/" + id).with(admin())).andExpect(status().isOk());
        AppUser stored = users.findByUsername("carol").orElseThrow();
        assertTrue(stored.getPasswordHash().startsWith("$2"));
        assertNotEquals("password1", stored.getPasswordHash());
        assertTrue(id.equals(stored.getId().toString()));
    }

    @Test
    void locationHeaderPointsToNewUser() throws Exception {
        create("{\"username\":\"loc1\",\"password\":\"password1\",\"role\":\"AGENT\"}")
                .andExpect(
                        header().string(
                                        "Location",
                                        org.hamcrest.Matchers.startsWith("/api/users/")));
    }

    @Test
    void createdUserCanLogIn() throws Exception {
        create("{\"username\":\"carol\",\"password\":\"password1\",\"role\":\"PLANNER\"}")
                .andExpect(status().isCreated());
        login("carol", "password1").andExpect(status().isOk());
    }

    @Test
    void validationErrorsReturn400() throws Exception {
        String[] bodies = {
            "{\"username\":\"\",\"password\":\"password1\",\"role\":\"AGENT\"}",
            "{\"username\":\"a b\",\"password\":\"password1\",\"role\":\"AGENT\"}",
            "{\"username\":\"ab\",\"password\":\"password1\",\"role\":\"AGENT\"}",
            "{\"username\":\"valid1\",\"password\":\"1234567\",\"role\":\"AGENT\"}",
            "{\"username\":\"valid1\",\"password\":\"password1\"}",
            "{\"username\":\"valid1\",\"password\":\"password1\",\"role\":\"ROOT\"}"
        };
        for (String b : bodies) {
            create(b).andExpect(status().isBadRequest());
        }
    }

    @Test
    void duplicateUsernameIsCaseInsensitive409() throws Exception {
        create("{\"username\":\"dave\",\"password\":\"password1\",\"role\":\"AGENT\"}")
                .andExpect(status().isCreated());
        create("{\"username\":\"DAVE\",\"password\":\"password1\",\"role\":\"AGENT\"}")
                .andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.ResultActions setActive(UUID id, boolean a)
            throws Exception {
        return mvc.perform(
                patch("/api/users/" + id + "/status")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":" + a + "}"));
    }

    @Test
    void deactivateAndReactivate() throws Exception {
        setActive(agentId, false).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        login("agent1", "secret123").andExpect(status().isUnauthorized());
        setActive(agentId, true).andExpect(status().isOk());
        login("agent1", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void adminCannotDeactivateSelf() throws Exception {
        setActive(adminId, false).andExpect(status().isConflict());
    }

    @Test
    void unknownUser404AndBadUuid400() throws Exception {
        mvc.perform(get("/api/users/" + UUID.randomUUID()).with(admin()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/users/not-a-uuid").with(admin())).andExpect(status().isBadRequest());
    }

    @Test
    void listIsSortedAndHidesHashes() throws Exception {
        mvc.perform(get("/api/users").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username").value(
                        org.hamcrest.Matchers.contains("admin1", "agent1", "planner1")))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }
}
