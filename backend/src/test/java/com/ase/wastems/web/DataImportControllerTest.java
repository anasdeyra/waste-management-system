package com.ase.wastems.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEvent;
import com.ase.wastems.repository.AuditEventRepository;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.CollectionPointRepository;
import com.ase.wastems.repository.DepotRepository;
import com.ase.wastems.repository.SourceSnapshotRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Integration tests of the MN01 import administration API. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataImportControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;
    @Autowired private CollectionPointRepository points;
    @Autowired private DepotRepository depots;
    @Autowired private SourceSnapshotRepository snapshots;
    @Autowired private AuditEventRepository auditEvents;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        points.deleteAll();
        depots.deleteAll();
        snapshots.deleteAll();
        agents.deleteAll();
        users.deleteAll();
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
    }

    @AfterEach
    void tearDown() {
        points.deleteAll();
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

    @Test
    void adminRunsTheImportAndReadsTheReport() throws Exception {
        mvc.perform(post("/api/admin/import").with(asAdmin()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("MANUAL"))
                .andExpect(jsonPath("$.baskets.rowCount").value(449))
                .andExpect(jsonPath("$.baskets.dsnyEligibleRowCount").value(238))
                .andExpect(jsonPath("$.depots.dsnyEligibleRowCount").doesNotExist())
                .andExpect(jsonPath("$.snapshots.length()").value(2))
                .andExpect(jsonPath("$.snapshots[0].sourceDataset").value("8znf-7b2c"));
        mvc.perform(get("/api/admin/import/report").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trigger").value("MANUAL"));
    }

    @Test
    void importIsAudited() throws Exception {
        jdbc.update("delete from audit_event");
        mvc.perform(post("/api/admin/import").with(asAdmin()).with(csrf())).andExpect(status().isOk());
        List<AuditEvent> imported =
                auditEvents.findAll().stream().filter(e -> e.getAction() == AuditAction.DATA_IMPORTED).toList();
        assertEquals(1, imported.size());
        AuditEvent e = imported.get(0);
        assertEquals("admin1", e.getActor());
        assertEquals(com.ase.wastems.domain.AuditEntityType.DATA_IMPORT, e.getEntityType());
        assertTrue(e.getEntityId().startsWith("8znf-7b2c:449:"), e.getEntityId());
        assertTrue(e.getDetails().startsWith("MANUAL: baskets created"), e.getDetails());
    }

    @Test
    void onlyAdminHasAccess() throws Exception {
        for (RequestPostProcessor who : new RequestPostProcessor[] {asPlanner(), asAgent()}) {
            mvc.perform(post("/api/admin/import").with(who).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/import/report").with(who))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/admin/import/report")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/import").with(asAdmin())).andExpect(status().isForbidden());
    }

    @Test
    void demoLoadDisabledOutsideDev() throws Exception {
        mvc.perform(post("/api/admin/demo").with(asAdmin()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Demo loading is disabled (wastems.demo.enabled=false)."));
    }
}
