package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.Agent;
import com.ase.wastems.domain.AppRole;
import com.ase.wastems.domain.AppUser;
import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEntityType;
import com.ase.wastems.domain.AuditEvent;
import com.ase.wastems.domain.AuditOutcome;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.repository.AuditEventRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Audit trail of sign-ins and of every S1-S7 data change (US-39, FR-30). */
class AuditTrailControllerTest extends ExecutionTestSupport {

    @Autowired private AuditEventRepository events;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void cleanAudit() {
        jdbc.update("delete from audit_event");
    }

    private List<AuditEvent> doFindEvents(AuditAction action) {
        return events.findAll().stream()
                .filter(e -> e.getAction() == action)
                .sorted(Comparator.comparing(AuditEvent::getOccurredAt))
                .toList();
    }

    private List<AuditEvent> doFindTrail(String entityId) {
        return events.findAll().stream().filter(e -> entityId.equals(e.getEntityId())).toList();
    }

    private ResultActions performJson(MockHttpServletRequestBuilder b, String json) throws Exception {
        return mvc.perform(b.with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private MvcResult doLogin(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    private String idOf(MvcResult r) throws Exception {
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void signInSuccessAndFailuresAreAudited() throws Exception {
        AppUser auditor = users.save(new AppUser("auditor1", encoder.encode("Right-Pass-1"), AppRole.PLANNER));
        AppUser sleeper = new AppUser("sleeper1", encoder.encode("Right-Pass-1"), AppRole.PLANNER);
        sleeper.setActive(false);
        users.save(sleeper);
        assertEquals(200, doLogin("auditor1", "Right-Pass-1").getResponse().getStatus());
        assertEquals(401, doLogin("auditor1", "Wrong-Pass-2").getResponse().getStatus());
        assertEquals(401, doLogin("ghost1", "Wrong-Pass-2").getResponse().getStatus());
        assertEquals(401, doLogin("sleeper1", "Right-Pass-1").getResponse().getStatus());
        List<AuditEvent> in = doFindEvents(AuditAction.SIGN_IN);
        assertEquals(4, in.size());
        AuditEvent ok = in.stream().filter(e -> e.getOutcome() == AuditOutcome.SUCCESS).findFirst().orElseThrow();
        assertEquals("auditor1", ok.getActor());
        assertEquals(AuditEntityType.USER, ok.getEntityType());
        assertEquals(auditor.getId().toString(), ok.getEntityId());
        assertEquals("role PLANNER", ok.getDetails());
        AuditEvent wrong = in.stream().filter(e -> "invalid credentials".equals(e.getDetails())
                && "auditor1".equals(e.getActor())).findFirst().orElseThrow();
        assertEquals(AuditOutcome.FAILURE, wrong.getOutcome());
        AuditEvent ghost = in.stream().filter(e -> e.getActor().equals("ghost1")).findFirst().orElseThrow();
        assertEquals(AuditOutcome.FAILURE, ghost.getOutcome());
        assertNull(ghost.getEntityId());
        AuditEvent dead = in.stream().filter(e -> e.getActor().equals("sleeper1")).findFirst().orElseThrow();
        assertEquals(AuditOutcome.FAILURE, dead.getOutcome());
        assertEquals("account deactivated", dead.getDetails());
    }

    @Test
    void signOutIsAudited() throws Exception {
        users.save(new AppUser("auditor1", encoder.encode("Right-Pass-1"), AppRole.PLANNER));
        MvcResult login = doLogin("auditor1", "Right-Pass-1");
        assertEquals(200, login.getResponse().getStatus());
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        List<AuditEvent> out = doFindEvents(AuditAction.SIGN_OUT);
        assertEquals(1, out.size());
        assertEquals("auditor1", out.get(0).getActor());
    }

    @Test
    void userAdministrationIsAudited() throws Exception {
        performJson(post("/api/users").with(asAdmin()),
                "{\"username\":\"newagent1\",\"password\":\"Create-Pass-3\",\"role\":\"AGENT\",\"agentId\":\""
                        + gus.getId() + "\"}").andExpect(status().isConflict());
        assertTrue(doFindEvents(AuditAction.USER_CREATED).isEmpty());
        Agent hal = agents.save(new Agent("Hal", DataOrigin.USER_ENTERED));
        MvcResult created = performJson(post("/api/users").with(asAdmin()),
                "{\"username\":\"newagent1\",\"password\":\"Create-Pass-3\",\"role\":\"AGENT\",\"agentId\":\""
                        + hal.getId() + "\"}").andExpect(status().isCreated()).andReturn();
        List<AuditEvent> c = doFindEvents(AuditAction.USER_CREATED);
        assertEquals(1, c.size());
        assertEquals("admin1", c.get(0).getActor());
        assertEquals("username newagent1, role AGENT", c.get(0).getDetails());
        assertEquals(idOf(created), c.get(0).getEntityId());
        List<AuditEvent> l = doFindEvents(AuditAction.AGENT_ACCOUNT_LINKED);
        assertEquals(1, l.size());
        assertEquals("linked to account newagent1", l.get(0).getDetails());
        performJson(patch("/api/users/" + idOf(created) + "/status").with(asAdmin()), "{\"active\":false}")
                .andExpect(status().isOk());
        List<AuditEvent> s = doFindEvents(AuditAction.USER_STATUS_CHANGED);
        assertEquals(1, s.size());
        assertEquals("deactivated", s.get(0).getDetails());
    }

    @Test
    void categoryChangesAreAudited() throws Exception {
        MvcResult c = performJson(post("/api/waste-categories").with(asAdmin()), "{\"name\":\"Metal\"}")
                .andExpect(status().isCreated()).andReturn();
        String id = idOf(c);
        performJson(put("/api/waste-categories/" + id).with(asAdmin()), "{\"name\":\"Metals\"}")
                .andExpect(status().isOk());
        performJson(patch("/api/waste-categories/" + id + "/status").with(asAdmin()), "{\"active\":false}")
                .andExpect(status().isOk());
        assertEquals("name Metal", doFindEvents(AuditAction.CATEGORY_CREATED).get(0).getDetails());
        assertEquals("name Metals", doFindEvents(AuditAction.CATEGORY_UPDATED).get(0).getDetails());
        assertEquals("deactivated", doFindEvents(AuditAction.CATEGORY_STATUS_CHANGED).get(0).getDetails());
        for (AuditAction a : List.of(AuditAction.CATEGORY_CREATED, AuditAction.CATEGORY_UPDATED,
                AuditAction.CATEGORY_STATUS_CHANGED)) {
            AuditEvent e = doFindEvents(a).get(0);
            assertEquals(AuditEntityType.WASTE_CATEGORY, e.getEntityType());
            assertEquals(id, e.getEntityId());
        }
    }

    @Test
    void pointAndDemandChangesAreAudited() throws Exception {
        MvcResult c = performJson(post("/api/collection-points").with(asPlanner()),
                "{\"name\":\"Audit point\",\"longitude\":-74.0,\"latitude\":40.71,\"demands\":[{\"categoryId\":\""
                        + paper.getId() + "\",\"quantityKg\":5},{\"categoryId\":\"" + glass.getId()
                        + "\",\"quantityKg\":2}]}").andExpect(status().isCreated()).andReturn();
        String id = idOf(c);
        assertTrue(doFindEvents(AuditAction.POINT_CREATED).get(0).getDetails().endsWith("2 demand entries"));
        performJson(put("/api/collection-points/" + id).with(asPlanner()),
                "{\"name\":\"Audit point 2\",\"longitude\":-74.0,\"latitude\":40.71}").andExpect(status().isOk());
        performJson(patch("/api/collection-points/" + id + "/status").with(asPlanner()), "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk());
        assertEquals("INACTIVE", doFindEvents(AuditAction.POINT_STATUS_CHANGED).get(0).getDetails());
        List<String> demandIds = JsonPath.read(c.getResponse().getContentAsString(), "$.demands[*].id");
        MvcResult added = performJson(post("/api/collection-points/" + id + "/demands").with(asPlanner()),
                "{\"categoryId\":\"" + oldCat.getId() + "\",\"quantityKg\":1}").andReturn();
        assertEquals(422, added.getResponse().getStatus());
        assertTrue(doFindEvents(AuditAction.DEMAND_ADDED).isEmpty());
        UUID metal = categories.save(new com.ase.wastems.domain.WasteCategory("Metal", null)).getId();
        MvcResult d = performJson(post("/api/collection-points/" + id + "/demands").with(asPlanner()),
                "{\"categoryId\":\"" + metal + "\",\"quantityKg\":3}").andExpect(status().isCreated()).andReturn();
        String demandId = idOf(d);
        performJson(put("/api/collection-points/" + id + "/demands/" + demandId).with(asPlanner()),
                "{\"categoryId\":\"" + metal + "\",\"quantityKg\":4}").andExpect(status().isOk());
        mvc.perform(delete("/api/collection-points/" + id + "/demands/" + demandId).with(asPlanner()).with(csrf()))
                .andExpect(status().isNoContent());
        assertEquals(1, doFindEvents(AuditAction.DEMAND_ADDED).size());
        assertEquals(1, doFindEvents(AuditAction.DEMAND_UPDATED).size());
        assertEquals("Metal (entry " + demandId + ")", doFindEvents(AuditAction.DEMAND_REMOVED).get(0).getDetails());
        assertTrue(doFindEvents(AuditAction.DEMAND_UPDATED).get(0).getDetails().startsWith("Metal 4"));
        assertEquals(2, demandIds.size());
        for (AuditAction a : List.of(AuditAction.POINT_UPDATED, AuditAction.DEMAND_ADDED,
                AuditAction.DEMAND_UPDATED, AuditAction.DEMAND_REMOVED)) {
            assertEquals(id, doFindEvents(a).get(0).getEntityId());
        }
    }

    @Test
    void vehicleAndAgentChangesAreAudited() throws Exception {
        MvcResult v = performJson(post("/api/vehicles").with(asPlanner()),
                "{\"registration\":\"ev-77\",\"capacityKg\":800}").andExpect(status().isCreated()).andReturn();
        performJson(put("/api/vehicles/" + idOf(v)).with(asPlanner()),
                "{\"registration\":\"EV-78\",\"capacityKg\":900}").andExpect(status().isOk());
        performJson(patch("/api/vehicles/" + idOf(v) + "/status").with(asPlanner()), "{\"status\":\"UNAVAILABLE\"}")
                .andExpect(status().isOk());
        assertEquals("EV-77, capacity 800 kg", doFindEvents(AuditAction.VEHICLE_CREATED).get(0).getDetails().replace(".00", ""));
        assertEquals("EV-78, capacity 900 kg", doFindEvents(AuditAction.VEHICLE_UPDATED).get(0).getDetails().replace(".00", ""));
        assertEquals("UNAVAILABLE", doFindEvents(AuditAction.VEHICLE_STATUS_CHANGED).get(0).getDetails());

        MvcResult a = performJson(post("/api/agents").with(asPlanner()), "{\"displayName\":\"Ivy\"}")
                .andExpect(status().isCreated()).andReturn();
        String agentId = idOf(a);
        performJson(put("/api/agents/" + agentId).with(asPlanner()), "{\"displayName\":\"Ivy B\"}")
                .andExpect(status().isOk());
        performJson(patch("/api/agents/" + agentId + "/status").with(asPlanner()), "{\"status\":\"UNAVAILABLE\"}")
                .andExpect(status().isOk());
        assertEquals("Ivy", doFindEvents(AuditAction.AGENT_CREATED).get(0).getDetails());
        assertEquals("Ivy B", doFindEvents(AuditAction.AGENT_UPDATED).get(0).getDetails());
        assertEquals("UNAVAILABLE", doFindEvents(AuditAction.AGENT_STATUS_CHANGED).get(0).getDetails());

        UUID agent5 = users.findByUsername("agent5").orElseThrow().getId();
        String link = "{\"userId\":\"" + agent5 + "\"}";
        performJson(put("/api/agents/" + agentId + "/user-account").with(asAdmin()), link).andExpect(status().isOk());
        performJson(put("/api/agents/" + agentId + "/user-account").with(asAdmin()), link).andExpect(status().isOk());
        assertEquals(1, doFindEvents(AuditAction.AGENT_ACCOUNT_LINKED).size());
        mvc.perform(delete("/api/agents/" + agentId + "/user-account").with(asAdmin()).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/agents/" + agentId + "/user-account").with(asAdmin()).with(csrf()))
                .andExpect(status().isNoContent());
        List<AuditEvent> un = doFindEvents(AuditAction.AGENT_ACCOUNT_UNLINKED);
        assertEquals(1, un.size());
        assertEquals("unlinked from account agent5", un.get(0).getDetails());
    }

    @Test
    void tourPlanningIsAudited() throws Exception {
        UUID id = doCreatePlanned();
        List<AuditEvent> trail = doFindTrail(id.toString());
        Map<AuditAction, Long> counts = trail.stream()
                .collect(Collectors.groupingBy(AuditEvent::getAction, Collectors.counting()));
        assertEquals(Map.of(AuditAction.TOUR_CREATED, 1L, AuditAction.TOUR_STOP_ADDED, 3L,
                AuditAction.TOUR_ASSIGNED, 1L, AuditAction.TOUR_EVALUATED, 1L,
                AuditAction.PROPOSAL_ACCEPTED, 1L, AuditAction.TOUR_CONFIRMED, 1L), counts);
        assertTrue(trail.stream().allMatch(e -> e.getActor().equals("planner1")));
        AuditEvent ev = trail.stream().filter(e -> e.getAction() == AuditAction.TOUR_EVALUATED).findFirst().orElseThrow();
        assertEquals(AuditOutcome.SUCCESS, ev.getOutcome());
        assertTrue(ev.getDetails().contains("COMPLETED on osm-mn01-"), ev.getDetails());
    }

    @Test
    void tourExecutionIsAudited() throws Exception {
        UUID id = doCreateInProgress();
        List<UUID> stops = stopIds(id);
        performOutcome(id, stops.get(0), "agent1", "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\""
                + paper.getId() + "\",\"actualQuantityKg\":5}]}").andExpect(status().isOk());
        performOutcome(id, stops.get(1), "agent1", "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\""
                + paper.getId() + "\",\"actualQuantityKg\":6}]}").andExpect(status().isOk());
        performOutcome(id, stops.get(2), "agent2", "{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"Blocked\"}")
                .andExpect(status().isOk());
        performComplete(id, "agent3").andExpect(status().isOk());
        List<AuditEvent> started = doFindEvents(AuditAction.TOUR_STARTED);
        assertEquals(1, started.size());
        assertEquals("agent1", started.get(0).getActor());
        List<AuditEvent> rec = doFindEvents(AuditAction.STOP_OUTCOME_RECORDED);
        assertEquals(3, rec.size());
        assertEquals(List.of("agent1", "agent1", "agent2"),
                rec.stream().map(AuditEvent::getActor).sorted().toList());
        assertEquals(1, rec.stream().filter(e -> e.getDetails().startsWith("stop 1 ")).count());
        List<AuditEvent> done = doFindEvents(AuditAction.TOUR_COMPLETED);
        assertEquals(1, done.size());
        assertEquals("agent3", done.get(0).getActor());
        assertTrue(done.get(0).getDetails().contains("2 collected, 1 not collected"), done.get(0).getDetails());
    }

    @Test
    void refusedOperationsLeaveNoEvent() throws Exception {
        UUID draft = doCreateDraft();
        UUID planned = doCreatePlanned();
        long before = events.count();
        mvc.perform(post("/api/tours/" + draft + "/confirmation").with(asPlanner()).with(csrf()))
                .andExpect(status().isUnprocessableEntity());
        performStart(draft, "agent1").andExpect(status().isForbidden());
        performComplete(planned, "agent1").andExpect(status().isConflict());
        mvc.perform(delete("/api/tours/" + draft).with(asPlanner()).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        assertEquals(before, events.count());
    }

    @Test
    void noPasswordOrHashInTheAuditTrail() throws Exception {
        performJson(post("/api/users").with(asAdmin()),
                "{\"username\":\"secret1\",\"password\":\"S3cret-Audit-77\",\"role\":\"PLANNER\"}")
                .andExpect(status().isCreated());
        assertEquals(401, doLogin("secret1", "Wr0ng-Audit-88").getResponse().getStatus());
        assertEquals(200, doLogin("secret1", "S3cret-Audit-77").getResponse().getStatus());
        String hash = users.findByUsername("secret1").orElseThrow().getPasswordHash();
        assertFalse(events.findAll().isEmpty());
        for (AuditEvent e : events.findAll()) {
            String text = e.getActor() + " " + e.getDetails();
            assertFalse(text.contains("S3cret-Audit-77"));
            assertFalse(text.contains("Wr0ng-Audit-88"));
            assertFalse(text.contains("$2a$"));
            assertFalse(text.contains(hash));
        }
    }
}
