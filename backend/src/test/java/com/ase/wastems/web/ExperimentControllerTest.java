package com.ase.wastems.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import com.ase.wastems.repository.TourRepository;
import com.ase.wastems.service.ImportTrigger;
import com.ase.wastems.service.Mn01ImportService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExperimentControllerTest {

    @Autowired MockMvc mvc;
    @Autowired AppUserRepository users;
    @Autowired EvaluationRunRepository evaluationRuns;
    @Autowired CollectionPointRepository points;
    @Autowired DepotRepository depots;
    @Autowired TourRepository tours;
    @Autowired SourceSnapshotRepository snapshots;
    @Autowired Mn01ImportService mn01Import;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        clean();
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
        mn01Import.importAll(ImportTrigger.MANUAL);
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    private void clean() {
        jdbc.update("delete from evaluation_batch");
        evaluationRuns.deleteAll();
        jdbc.update("delete from audit_event");
        tours.deleteAll();
        points.deleteAll();
        depots.deleteAll();
        snapshots.deleteAll();
        users.deleteAll();
    }

    private org.springframework.test.web.servlet.ResultActions performRun(String json) throws Exception {
        return mvc.perform(post("/api/admin/experiments").with(user("admin1").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void adminRunsASmallBatch() throws Exception {
        performRun("{\"profiles\":[\"HOTSPOT\"],\"seeds\":[2],\"sizes\":[4]}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.matchesPattern(".*/api/evaluations/batches/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.kind").value("EXPERIMENT"))
                .andExpect(jsonPath("$.runCount").value(1))
                .andExpect(jsonPath("$.completedRunCount").value(1))
                .andExpect(jsonPath("$.canonical").value(false))
                .andExpect(jsonPath("$.label").value("MN01 experiment subset"))
                .andExpect(jsonPath("$.demandProfiles[0]").value("HOTSPOT"))
                .andExpect(jsonPath("$.seeds[0]").value(2))
                .andExpect(jsonPath("$.sizes[0]").value(4))
                .andExpect(jsonPath("$.repetitions").value(1))
                .andExpect(jsonPath("$.vehicleCapacityKg").value(10000.0))
                .andExpect(jsonPath("$.environment", Matchers.startsWith("Java ")));
    }

    @Test
    void validationErrorsAre400() throws Exception {
        performRun("{\"seeds\":[0]}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("seeds"));
        performRun("{\"sizes\":[101]}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("sizes"));
        performRun("{\"repetitions\":4}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("repetitions"));
        performRun("{\"profiles\":[\"NOPE\"]}").andExpect(status().isBadRequest());
    }

    @Test
    void rolesAndMethods() throws Exception {
        String body = "{\"profiles\":[\"HOTSPOT\"],\"seeds\":[2],\"sizes\":[4]}";
        mvc.perform(post("/api/admin/experiments").with(user("planner1").roles("PLANNER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/experiments").with(user("agent1").roles("AGENT")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/experiments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/experiments").with(user("admin1").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/experiments").with(user("admin1").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
    }
}
