package com.ase.wastems.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.Depot;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.WasteCategoryRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests of the read-only depot API. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepotControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;
    @Autowired private WasteCategoryRepository categories;
    @Autowired private CollectionPointRepository points;
    @Autowired private DepotRepository depots;

    private UUID depotId;

    @BeforeEach
    void setUp() {
        points.deleteAll();
        depots.deleteAll();
        categories.deleteAll();
        agents.deleteAll();
        users.deleteAll();
        users.save(new AppUser("p", "x", AppRole.PLANNER));
        users.save(new AppUser("a", "x", AppRole.ADMIN));
        users.save(new AppUser("g", "x", AppRole.AGENT));
        depotId =
                depots.save(
                                new Depot(
                                        "MN01G Garage, 353 Spring St",
                                        -74.0104183829902,
                                        40.7255225360961,
                                        DataOrigin.IMPORTED_REAL,
                                        "xw3j-2yxf",
                                        "MN01G"))
                        .getId();
    }

    @Test
    void plannerAndAdminCanListDepots() throws Exception {
        mvc.perform(get("/api/depots").with(user("p").roles("PLANNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sourceIdentifier").value("MN01G"))
                .andExpect(jsonPath("$[0].origin").value("IMPORTED_REAL"));
        mvc.perform(get("/api/depots").with(user("a").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void agentAndAnonymousAreRefused() throws Exception {
        mvc.perform(get("/api/depots").with(user("g").roles("AGENT")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/depots")).andExpect(status().isUnauthorized());
    }

    @Test
    void getByIdAndNotFound() throws Exception {
        mvc.perform(get("/api/depots/" + depotId).with(user("p").roles("PLANNER")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/depots/" + UUID.randomUUID()).with(user("p").roles("PLANNER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void depotsAreReadOnly() throws Exception {
        mvc.perform(
                        post("/api/depots")
                                .with(user("p").roles("PLANNER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
