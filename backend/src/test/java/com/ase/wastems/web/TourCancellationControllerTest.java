package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** Cancellation of draft, planned and in-progress tours with a reason (S8, US-30, D-025). */
class TourCancellationControllerTest extends ExecutionTestSupport {

    @Autowired private AuditEventRepository events;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void cleanAudit() {
        jdbc.update("delete from audit_event");
    }

    private ResultActions performCancel(UUID id, String json, RequestPostProcessor who) throws Exception {
        return mvc.perform(post("/api/tours/" + id + "/cancellation").with(who).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private List<AuditEvent> doFindCancellations() {
        return events.findAll().stream().filter(e -> e.getAction() == AuditAction.TOUR_CANCELLED).toList();
    }

    private String collected(UUID cat, String kg) {
        return "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\"" + cat
                + "\",\"actualQuantityKg\":" + kg + "}]}";
    }

    @Test
    void cancelDraftWithReason() throws Exception {
        UUID id = doCreateDraft();
        long rev = readTour(id).get("revision").asLong();
        performCancel(id, "{\"reason\":\"  Street fair on Broadway  \"}", asPlanner())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Street fair on Broadway"))
                .andExpect(jsonPath("$.cancelledBy").value("planner1"))
                .andExpect(jsonPath("$.cancelledAt").exists())
                .andExpect(jsonPath("$.cancelledFromStatus").value("DRAFT"))
                .andExpect(jsonPath("$.revision").value(rev + 1))
                .andExpect(jsonPath("$.cancellable").value(false))
                .andExpect(jsonPath("$.correctable").value(true))
                .andExpect(jsonPath("$.stops.length()").value(3))
                .andExpect(jsonPath("$.readyToConfirm").value(false))
                .andExpect(jsonPath("$.confirmationIssues.length()").value(0));
        List<AuditEvent> ev = doFindCancellations();
        assertEquals(1, ev.size());
        assertEquals("planner1", ev.get(0).getActor());
        assertEquals(id.toString(), ev.get(0).getEntityId());
        assertEquals(true, ev.get(0).getDetails().startsWith("from DRAFT; 0 of 3 stops resolved; reason: Street fair"),
                ev.get(0).getDetails());
    }

    @Test
    void cancelPlannedAndInProgressKeepTheRecord() throws Exception {
        UUID planned = doCreatePlanned();
        performCancel(planned, "{\"reason\":\"Vehicle breakdown\"}", asPlanner()).andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledFromStatus").value("PLANNED"))
                .andExpect(jsonPath("$.route").exists());
        UUID running = doCreateInProgress();
        UUID s1 = stopOfPoint(running, p1);
        performOutcome(running, s1, "agent1", collected(paper.getId(), "7.5")).andExpect(status().isOk());
        performCancel(running, "{\"reason\":\"Snow emergency\"}", asPlanner()).andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledFromStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedBy").value("agent1"));
        JsonNode t = readTour(running);
        for (JsonNode s : t.get("stops")) {
            if (s.get("id").asText().equals(s1.toString())) {
                assertEquals("COLLECTED", s.get("status").asText());
                assertEquals("agent1", s.get("outcomeRecordedBy").asText());
                assertEquals(1, s.get("actualQuantities").size());
            } else {
                assertEquals("PENDING", s.get("status").asText());
            }
        }
    }

    @Test
    void completedOrCancelledToursCannotBeCancelled() throws Exception {
        UUID done = doCreateInProgress();
        for (UUID s : stopIds(done)) {
            performOutcome(done, s, "agent1", collected(paper.getId(), "1")).andExpect(status().isOk());
        }
        performComplete(done, "agent1").andExpect(status().isOk());
        performCancel(done, "{\"reason\":\"Too late\"}", asPlanner()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Only a DRAFT, PLANNED or IN_PROGRESS tour can be cancelled; this tour is COMPLETED."));
        UUID draft = doCreateDraft();
        performCancel(draft, "{\"reason\":\"First\"}", asPlanner()).andExpect(status().isOk());
        performCancel(draft, "{\"reason\":\"Again\"}", asPlanner()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Only a DRAFT, PLANNED or IN_PROGRESS tour can be cancelled; this tour is CANCELLED."));
        assertEquals(1, doFindCancellations().size());
    }

    @Test
    void cancellationValidation() throws Exception {
        UUID id = doCreateDraft();
        long rev = readTour(id).get("revision").asLong();
        mvc.perform(post("/api/tours/" + id + "/cancellation").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        performCancel(id, "{}", asPlanner()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"));
        performCancel(id, "{\"reason\":\"   \"}", asPlanner()).andExpect(status().isBadRequest());
        performCancel(id, "{\"reason\":\"" + "x".repeat(501) + "\"}", asPlanner()).andExpect(status().isBadRequest());
        performCancel(UUID.randomUUID(), "{\"reason\":\"x\"}", asPlanner()).andExpect(status().isNotFound());
        mvc.perform(post("/api/tours/not-a-uuid/cancellation").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isBadRequest());
        JsonNode t = readTour(id);
        assertEquals("DRAFT", t.get("status").asText());
        assertEquals(rev, t.get("revision").asLong());
        assertEquals(0, doFindCancellations().size());
    }

    @Test
    void cancellationRoles() throws Exception {
        UUID id = doCreatePlanned();
        performCancel(id, "{\"reason\":\"x\"}", asAgent("agent1")).andExpect(status().isForbidden());
        assertEquals("PLANNED", readTour(id).get("status").asText());
        mvc.perform(post("/api/tours/" + id + "/cancellation").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        performCancel(id, "{\"reason\":\"Admin decision\"}", asAdmin()).andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledBy").value("admin1"));
    }

    @Test
    void cancelledTourIsFrozen() throws Exception {
        UUID id = doCreateInProgress();
        UUID pending = stopOfPoint(id, p2);
        performCancel(id, "{\"reason\":\"Frozen\"}", asPlanner()).andExpect(status().isOk());
        mvc.perform(post("/api/tours/" + id + "/stops").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"collectionPointId\":\"" + p1.getId() + "\"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/tours/" + id + "/assignment").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehicleId\":\"" + big.getId() + "\",\"driverId\":\"" + dana.getId()
                        + "\",\"collectorIds\":[]}")).andExpect(status().isConflict());
        mvc.perform(post("/api/tours/" + id + "/evaluations").with(asPlanner()).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/tours/" + id + "/confirmation").with(asPlanner()).with(csrf()))
                .andExpect(status().isConflict());
        performStart(id, "agent1").andExpect(status().isConflict());
        performOutcome(id, pending, "agent1", collected(paper.getId(), "1")).andExpect(status().isConflict());
        performComplete(id, "agent1").andExpect(status().isConflict());
        mvc.perform(delete("/api/tours/" + id).with(asPlanner()).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void crewSeesOnlyConfirmedCancelledTours() throws Exception {
        UUID a = doCreateDraft();
        UUID b = doCreatePlanned();
        performCancel(a, "{\"reason\":\"Draft dropped\"}", asPlanner()).andExpect(status().isOk());
        performCancel(b, "{\"reason\":\"Plan dropped\"}", asPlanner()).andExpect(status().isOk());
        performMine("agent1").andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(b.toString()))
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));
        performMineOne(a, "agent1").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not assigned to this tour."));
        performMineOne(b, "agent1").andExpect(status().isOk())
                .andExpect(jsonPath("$.cancellationReason").value("Plan dropped"));
    }
}
