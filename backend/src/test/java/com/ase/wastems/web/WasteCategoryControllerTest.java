package com.ase.wastems.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
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

/** Integration tests of the waste category API. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WasteCategoryControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private WasteCategoryRepository categories;
    @Autowired private CollectionPointRepository points;
    @Autowired private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        points.deleteAll();
        categories.deleteAll();
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
    }

    private static RequestPostProcessor admin() {
        return user("admin1").roles("ADMIN");
    }

    private static RequestPostProcessor planner() {
        return user("planner1").roles("PLANNER");
    }

    private static RequestPostProcessor agent() {
        return user("agent1").roles("AGENT");
    }

    private ResultActions create(RequestPostProcessor who, String body) throws Exception {
        return mvc.perform(
                post("/api/waste-categories")
                        .with(who)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }

    private String createAs(String name) throws Exception {
        String res =
                create(admin(), "{\"name\":\"" + name + "\"}")
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return mapper.readTree(res).get("id").asText();
    }

    private ResultActions rename(String id, String name) throws Exception {
        return mvc.perform(
                put("/api/waste-categories/" + id)
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"));
    }

    private ResultActions setStatus(String id, boolean active) throws Exception {
        return mvc.perform(
                patch("/api/waste-categories/" + id + "/status")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":" + active + "}"));
    }

    @Test
    void adminCreatesCategory() throws Exception {
        create(admin(), "{\"name\":\"Paper\",\"description\":\"Cardboard and paper\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.startsWith("/api/waste-categories/")))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void roleRestrictions() throws Exception {
        create(planner(), "{\"name\":\"Paper\"}").andExpect(status().isForbidden());
        create(agent(), "{\"name\":\"Paper\"}").andExpect(status().isForbidden());
        String id = createAs("Glass");
        mvc.perform(
                        put("/api/waste-categories/" + id)
                                .with(planner())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        patch("/api/waste-categories/" + id + "/status")
                                .with(agent())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/waste-categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void plannerAndAgentCanRead() throws Exception {
        mvc.perform(get("/api/waste-categories").with(planner())).andExpect(status().isOk());
        mvc.perform(get("/api/waste-categories").with(agent())).andExpect(status().isOk());
    }

    @Test
    void validation() throws Exception {
        create(admin(), "{\"name\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
        create(admin(), "{\"name\":\"" + "a".repeat(81) + "\"}").andExpect(status().isBadRequest());
        create(admin(), "{\"name\":\"Paper\",\"description\":\"" + "a".repeat(501) + "\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateNameIsConflict() throws Exception {
        createAs("Paper");
        create(admin(), "{\"name\":\"paper\"}").andExpect(status().isConflict());
    }

    @Test
    void renameKeepsIdAndDetectsConflicts() throws Exception {
        String id = createAs("Paper");
        String other = createAs("Glass");
        rename(id, "Paper & cardboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        rename(other, "Paper & cardboard").andExpect(status().isConflict());
        rename(other, "GLASS").andExpect(status().isOk());
    }

    @Test
    void deactivationHidesFromActiveListButKeepsHistory() throws Exception {
        String id = createAs("Paper");
        setStatus(id, false).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/waste-categories?active=true").with(planner()))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/waste-categories").with(planner()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
        mvc.perform(get("/api/waste-categories/" + id).with(planner()))
                .andExpect(status().isOk());
    }

    @Test
    void unknownIdIs404() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(get("/api/waste-categories/" + id).with(admin())).andExpect(status().isNotFound());
        rename(id, "X").andExpect(status().isNotFound());
        setStatus(id, true).andExpect(status().isNotFound());
    }
}
