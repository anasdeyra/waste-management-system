package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.AuditAction;
import com.ase.wastems.domain.AuditEvent;
import com.ase.wastems.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Privileged correction of recorded stop outcomes with a retained history (S8, US-31, D-027). */
class StopOutcomeCorrectionControllerTest extends ExecutionTestSupport {

    @Autowired private AuditEventRepository events;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanAudit() {
        jdbc.update("delete from audit_event");
    }

    private ResultActions performCorrect(UUID tour, UUID stop, String json, RequestPostProcessor who)
            throws Exception {
        return mvc.perform(post("/api/tours/" + tour + "/stops/" + stop + "/corrections").with(who).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String q(UUID cat, String kg) {
        return "{\"categoryId\":\"" + cat + "\",\"actualQuantityKg\":" + kg + "}";
    }

    private static String collected(String reason, String... quantities) {
        return "{\"status\":\"COLLECTED\",\"quantities\":[" + String.join(",", quantities)
                + "],\"reason\":\"" + reason + "\"}";
    }

    /** A completed tour: A (p1) paper 10 + glass 2, B (p2) not collected, C (p3) paper 4 + glass 1. */
    private UUID doCompleteTour() throws Exception {
        UUID id = doCreateInProgress();
        performOutcome(id, stopOfPoint(id, p1), "agent1", "{\"status\":\"COLLECTED\",\"quantities\":["
                + q(paper.getId(), "10") + "," + q(glass.getId(), "2") + "]}").andExpect(status().isOk());
        performOutcome(id, stopOfPoint(id, p2), "agent2",
                "{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"Blocked\"}").andExpect(status().isOk());
        performOutcome(id, stopOfPoint(id, p3), "agent1", "{\"status\":\"COLLECTED\",\"quantities\":["
                + q(paper.getId(), "4") + "," + q(glass.getId(), "1") + "]}").andExpect(status().isOk());
        performComplete(id, "agent1").andExpect(status().isOk());
        return id;
    }

    private JsonNode stopJson(JsonNode tour, UUID stop) {
        for (JsonNode s : tour.get("stops")) {
            if (s.get("id").asText().equals(stop.toString())) {
                return s;
            }
        }
        throw new AssertionError("stop not found");
    }

    private int auditCount() {
        return (int) events.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.STOP_OUTCOME_CORRECTED).count();
    }

    @Test
    void plannerCorrectsCollectedQuantities() throws Exception {
        UUID tour = doCompleteTour();
        UUID a = stopOfPoint(tour, p1);
        long rev = readTour(tour).get("revision").asLong();
        performCorrect(tour, a, collected("Weighbridge ticket 12.5 kg", q(paper.getId(), "12.5")), asPlanner())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.revision").value(rev + 1));
        JsonNode s = stopJson(readTour(tour), a);
        assertEquals("COLLECTED", s.get("status").asText());
        assertEquals(1, s.get("actualQuantities").size());
        assertEquals("Paper", s.get("actualQuantities").get(0).get("categoryName").asText());
        assertEquals(12.5, s.get("actualQuantities").get(0).get("actualQuantityKg").asDouble());
        assertEquals(12.5, s.get("actualTotalKg").asDouble());
        assertEquals("agent1", s.get("outcomeRecordedBy").asText());
        JsonNode c = s.get("corrections").get(0);
        assertEquals("planner1", c.get("correctedBy").asText());
        assertEquals("Weighbridge ticket 12.5 kg", c.get("reason").asText());
        assertEquals("COLLECTED", c.get("previousStatus").asText());
        assertEquals("Glass", c.get("previousQuantities").get(0).get("categoryName").asText());
        assertEquals(2.0, c.get("previousQuantities").get(0).get("quantityKg").asDouble());
        assertEquals("Paper", c.get("previousQuantities").get(1).get("categoryName").asText());
        assertEquals(10.0, c.get("previousQuantities").get(1).get("quantityKg").asDouble());
        assertEquals("agent1", c.get("previousRecordedBy").asText());
        assertEquals("COLLECTED", c.get("newStatus").asText());
        assertEquals(1, c.get("newQuantities").size());
        assertEquals(12.5, c.get("newQuantities").get(0).get("quantityKg").asDouble());
        List<AuditEvent> ev = events.findAll().stream()
                .filter(e -> e.getAction() == AuditAction.STOP_OUTCOME_CORRECTED).toList();
        assertEquals(1, ev.size());
        assertEquals("planner1", ev.get(0).getActor());
        assertEquals(tour.toString(), ev.get(0).getEntityId());
        assertTrue(ev.get(0).getDetails().contains("correction " + c.get("id").asText()), ev.get(0).getDetails());
        assertTrue(ev.get(0).getDetails().contains("COLLECTED -> COLLECTED"));
        assertTrue(ev.get(0).getDetails().contains("reason: Weighbridge ticket"));
    }

    @Test
    void notCollectedBecomesCollectedAndBackKeepsBothCorrections() throws Exception {
        UUID tour = doCompleteTour();
        UUID b = stopOfPoint(tour, p2);
        performCorrect(tour, b, collected("First check", q(paper.getId(), "3")), asPlanner())
                .andExpect(status().isOk());
        performCorrect(tour, b,
                "{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"Bin missing\",\"reason\":\"Second check\"}",
                asPlanner()).andExpect(status().isOk());
        JsonNode s = stopJson(readTour(tour), b);
        assertEquals(2, s.get("corrections").size());
        JsonNode c1 = s.get("corrections").get(0);
        JsonNode c2 = s.get("corrections").get(1);
        assertEquals("NOT_COLLECTED", c1.get("previousStatus").asText());
        assertEquals("COLLECTED", c1.get("newStatus").asText());
        assertEquals("Blocked", c1.get("previousIncidentReason").asText());
        assertEquals("COLLECTED", c2.get("previousStatus").asText());
        assertEquals("NOT_COLLECTED", c2.get("newStatus").asText());
        assertEquals(1, c2.get("previousQuantities").size());
        assertEquals(3.0, c2.get("previousQuantities").get(0).get("quantityKg").asDouble());
        assertEquals("NOT_COLLECTED", s.get("status").asText());
        assertEquals("Bin missing", s.get("incidentReason").asText());
        assertEquals(0, s.get("actualQuantities").size());
    }

    @Test
    void correctionBodyValidation() throws Exception {
        UUID tour = doCompleteTour();
        UUID a = stopOfPoint(tour, p1);
        String pq = q(paper.getId(), "1");
        String[][] cases = {
            {"{\"status\":\"COLLECTED\",\"quantities\":[" + pq + "]}", null},
            {"{\"status\":\"COLLECTED\",\"quantities\":[" + pq + "],\"reason\":\"  \"}", null},
            {"{\"status\":\"COLLECTED\",\"quantities\":[" + pq + "],\"reason\":\"" + "r".repeat(501) + "\"}", null},
            {"{\"status\":\"PENDING\",\"reason\":\"r\"}", "status"},
            {"{\"status\":\"COLLECTED\",\"reason\":\"r\"}", "quantities"},
            {"{\"status\":\"NOT_COLLECTED\",\"reason\":\"r\"}", "incidentReason"},
            {"{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"x\",\"quantities\":[" + pq + "],\"reason\":\"r\"}",
                "quantities"},
            {collected("r", q(paper.getId(), "-1")), null},
        };
        for (String[] c : cases) {
            ResultActions r = performCorrect(tour, a, c[0], asPlanner()).andExpect(status().isBadRequest());
            if (c[1] != null) {
                r.andExpect(jsonPath("$.fieldErrors[0].field").value(c[1]));
            }
        }
        assertEquals(0, stopJson(readTour(tour), a).get("corrections").size());
        assertEquals(0, auditCount());
    }

    @Test
    void correctionRuleOrder() throws Exception {
        UUID tour = doCompleteTour();
        UUID a = stopOfPoint(tour, p1);
        String ok = collected("r", q(paper.getId(), "11"));
        performCorrect(UUID.randomUUID(), a, ok, asPlanner()).andExpect(status().isNotFound());
        UUID other = doCreateDraft();
        performCorrect(tour, stopOfPoint(other, p1), ok, asPlanner()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tour stop not found: " + stopOfPoint(other, p1)));
        UUID planned = doCreatePlanned();
        performCorrect(planned, stopOfPoint(planned, p1), ok, asPlanner()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Outcomes can be corrected only on an IN_PROGRESS, COMPLETED or CANCELLED tour; this tour is PLANNED."));
        UUID running = doCreateInProgress();
        performCorrect(running, stopOfPoint(running, p1), ok, asPlanner()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith("Stop "),
                        org.hamcrest.Matchers.endsWith("has no recorded outcome to correct."))));
        performCorrect(tour, a, collected("r", q(UUID.randomUUID(), "1")), asPlanner())
                .andExpect(status().isNotFound());
        performCorrect(tour, a, collected("r", q(paper.getId(), "1"), q(paper.getId(), "2")), asPlanner())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Each waste category may appear only once."));
        performCorrect(tour, a, collected("r", q(oldCat.getId(), "1")), asPlanner())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Waste category Old category is inactive."));
        performCorrect(tour, a,
                "{\"status\":\"COLLECTED\",\"incidentReason\":\" \",\"quantities\":[" + q(glass.getId(), "2")
                        + "," + q(paper.getId(), "10.00") + "],\"reason\":\"same\"}", asPlanner())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("The correction does not change the recorded outcome."));
        assertEquals(0, stopJson(readTour(tour), a).get("corrections").size());
        assertEquals(0, auditCount());
    }

    @Test
    void inactiveCategoryAlreadyRecordedMayBeKept() throws Exception {
        UUID tour = doCompleteTour();
        glass.setActive(false);
        categories.save(glass);
        performCorrect(tour, stopOfPoint(tour, p3), collected("Reweighed", q(paper.getId(), "5"), q(glass.getId(), "1")),
                asPlanner()).andExpect(status().isOk());
        performCorrect(tour, stopOfPoint(tour, p2), collected("Oops", q(glass.getId(), "1")), asPlanner())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Waste category Glass is inactive."));
    }

    @Test
    void onlyPlannersAndAdminsCorrect() throws Exception {
        UUID tour = doCompleteTour();
        UUID a = stopOfPoint(tour, p1);
        String body = collected("r", q(paper.getId(), "11"));
        performCorrect(tour, a, body, asAgent("agent1")).andExpect(status().isForbidden());
        assertEquals(0, stopJson(readTour(tour), a).get("corrections").size());
        mvc.perform(post("/api/tours/" + tour + "/stops/" + a + "/corrections").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        performCorrect(tour, a, body, asAdmin()).andExpect(status().isOk());
        assertEquals("admin1", stopJson(readTour(tour), a).get("corrections").get(0).get("correctedBy").asText());
    }

    @Test
    void inProgressAndCancelledToursAreCorrectable() throws Exception {
        UUID tour = doCreateInProgress();
        UUID a = stopOfPoint(tour, p1);
        performOutcome(tour, a, "agent1", "{\"status\":\"COLLECTED\",\"quantities\":[" + q(paper.getId(), "6") + "]}")
                .andExpect(status().isOk());
        performCorrect(tour, a, collected("Typo", q(paper.getId(), "9")), asPlanner()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.readyToComplete").value(false));
        performOutcome(tour, a, "agent1", "{\"status\":\"COLLECTED\",\"quantities\":[" + q(paper.getId(), "1") + "]}")
                .andExpect(status().isConflict());
        mvc.perform(post("/api/tours/" + tour + "/cancellation").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Stopped\"}"))
                .andExpect(status().isOk());
        performCorrect(tour, a, collected("After cancel", q(paper.getId(), "8")), asPlanner())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(2, stopJson(readTour(tour), a).get("corrections").size());
    }

    @Test
    void crewSeesCorrections() throws Exception {
        UUID tour = doCompleteTour();
        UUID a = stopOfPoint(tour, p1);
        performCorrect(tour, a, collected("Weighbridge", q(paper.getId(), "12.5")), asPlanner())
                .andExpect(status().isOk());
        JsonNode s = stopJson(mapper.readTree(performMineOne(tour, "agent1").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()), a);
        assertEquals("planner1", s.get("corrections").get(0).get("correctedBy").asText());
    }
}
