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
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditEvent;
import com.ase.wastems.domain.AuditOutcome;
import com.ase.wastems.repository.AgentRepository;
import com.ase.wastems.repository.AppUserRepository;
import com.ase.wastems.repository.AuditEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** The read-only audit log API of administrators (S8, US-39, US-03). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditEventControllerTest {

    private static final Instant T0 = Instant.parse("2026-10-01T08:00:00Z");

    @Autowired private MockMvc mvc;
    @Autowired private AuditEventRepository events;
    @Autowired private AppUserRepository users;
    @Autowired private AgentRepository agents;
    @Autowired private JdbcTemplate jdbc;

    private List<AuditEvent> saved;

    private static RequestPostProcessor admin() {
        return user("admin1").roles("ADMIN");
    }

    private static String iso(int minutes) {
        return T0.plusSeconds(60L * minutes).toString();
    }

    @BeforeEach
    void setUp() {
        jdbc.update("delete from audit_event");
        agents.deleteAll();
        users.deleteAll();
        users.save(new AppUser("admin1", "x", AppRole.ADMIN));
        users.save(new AppUser("planner1", "x", AppRole.PLANNER));
        users.save(new AppUser("agent1", "x", AppRole.AGENT));
        saved = List.of(
                events.save(new AuditEvent(T0, "planner1", AuditAction.VEHICLE_CREATED, AuditEntityType.VEHICLE, "v-1",
                        AuditOutcome.SUCCESS, "EV-1")),
                events.save(new AuditEvent(T0.plusSeconds(60), "ghost", AuditAction.SIGN_IN, AuditEntityType.USER, null,
                        AuditOutcome.FAILURE, "invalid credentials")),
                events.save(new AuditEvent(T0.plusSeconds(120), "planner1", AuditAction.TOUR_CANCELLED,
                        AuditEntityType.TOUR, "t-1", AuditOutcome.SUCCESS, "from DRAFT")),
                events.save(new AuditEvent(T0.plusSeconds(180), "admin1", AuditAction.USER_CREATED,
                        AuditEntityType.USER, "u-1", AuditOutcome.SUCCESS, "username x")),
                events.save(new AuditEvent(T0.plusSeconds(240), "agent1", AuditAction.TOUR_STARTED,
                        AuditEntityType.TOUR, "t-1", AuditOutcome.SUCCESS, "IN_PROGRESS")),
                events.save(new AuditEvent(T0.plusSeconds(300), "ghost", AuditAction.SIGN_IN, AuditEntityType.USER, null,
                        AuditOutcome.FAILURE, "invalid credentials")));
    }

    @AfterEach
    void tearDown() {
        jdbc.update("delete from audit_event");
        users.deleteAll();
    }

    @Test
    void adminListsNewestFirstWithPaging() throws Exception {
        mvc.perform(get("/api/admin/audit-events").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.items[0].action").value("SIGN_IN"))
                .andExpect(jsonPath("$.items[0].occurredAt").value("2026-10-01T08:05:00Z"))
                .andExpect(jsonPath("$.items[5].action").value("VEHICLE_CREATED"))
                .andExpect(jsonPath("$.items[0].entityId").doesNotExist());
        mvc.perform(get("/api/admin/audit-events?page=1&size=4").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(4));
    }

    @Test
    void filtersByTimeActorActionEntityAndOutcome() throws Exception {
        mvc.perform(get("/api/admin/audit-events?from=" + iso(1) + "&to=" + iso(3)).with(admin()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].occurredAt").value("2026-10-01T08:02:00Z"))
                .andExpect(jsonPath("$.items[1].occurredAt").value("2026-10-01T08:01:00Z"));
        mvc.perform(get("/api/admin/audit-events?actor=PLANNER1").with(admin()))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/admin/audit-events?action=TOUR_CANCELLED").with(admin()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/admin/audit-events?entityType=TOUR&entityId=t-1").with(admin()))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/admin/audit-events?outcome=FAILURE").with(admin()))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].action").value("SIGN_IN"))
                .andExpect(jsonPath("$.items[1].action").value("SIGN_IN"));
        mvc.perform(get("/api/admin/audit-events?actor=ghost&outcome=SUCCESS").with(admin()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void invalidQueriesAre400() throws Exception {
        for (String q : new String[] {"action=NOPE", "outcome=x", "from=yesterday", "size=0", "size=201", "page=-1",
                "from=" + iso(3) + "&to=" + iso(1), "from=" + iso(0) + "&to=" + iso(0)}) {
            mvc.perform(get("/api/admin/audit-events?" + q).with(admin())).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/admin/audit-events?from=" + iso(3) + "&to=" + iso(1)).with(admin()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("to"));
        mvc.perform(get("/api/admin/audit-events?from=" + iso(0) + "&to=" + iso(0)).with(admin()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("to"));
        mvc.perform(get("/api/admin/audit-events?size=0").with(admin()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
        mvc.perform(get("/api/admin/audit-events?page=-1").with(admin()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("page"));
    }

    @Test
    void onlyAdminsReadTheLog() throws Exception {
        String one = "/api/admin/audit-events/" + saved.get(0).getId();
        for (String url : new String[] {"/api/admin/audit-events", one}) {
            mvc.perform(get(url).with(user("planner1").roles("PLANNER"))).andExpect(status().isForbidden());
            mvc.perform(get(url).with(user("agent1").roles("AGENT"))).andExpect(status().isForbidden());
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void auditEventsCannotBeChanged() throws Exception {
        UUID id = saved.get(0).getId();
        String url = "/api/admin/audit-events/" + id;
        mvc.perform(put(url).with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(patch(url).with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(url).with(admin()).with(csrf())).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/admin/audit-events").with(admin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isMethodNotAllowed());
        assertEquals(6, events.count());
        AuditEvent e = events.findById(id).orElseThrow();
        assertEquals("EV-1", e.getDetails());
        assertEquals("planner1", e.getActor());
    }

    @Test
    void singleEventById() throws Exception {
        AuditEvent e = saved.get(2);
        mvc.perform(get("/api/admin/audit-events/" + e.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(e.getId().toString()))
                .andExpect(jsonPath("$.actor").value("planner1"))
                .andExpect(jsonPath("$.action").value("TOUR_CANCELLED"))
                .andExpect(jsonPath("$.entityType").value("TOUR"))
                .andExpect(jsonPath("$.entityId").value("t-1"))
                .andExpect(jsonPath("$.outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.details").value("from DRAFT"))
                .andExpect(jsonPath("$.occurredAt").value("2026-10-01T08:02:00Z"));
        UUID unknown = UUID.randomUUID();
        mvc.perform(get("/api/admin/audit-events/" + unknown).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Audit event not found: " + unknown));
        mvc.perform(get("/api/admin/audit-events/not-a-uuid").with(admin())).andExpect(status().isBadRequest());
    }
}
