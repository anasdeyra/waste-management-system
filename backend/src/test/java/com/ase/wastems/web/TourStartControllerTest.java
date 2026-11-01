package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.Tour;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Integration tests of the start gate (S7, US-27, US-14, US-17, US-12). */
class TourStartControllerTest extends ExecutionTestSupport {

    private static final String ALREADY = "Only a PLANNED tour can be started; this tour is IN_PROGRESS.";

    private void doPatch(String path, String body) throws Exception {
        mvc.perform(patch(path).with(asPlanner()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isOk());
    }

    private void doPointStatus(UUID pointId, String st) throws Exception {
        doPatch("/api/collection-points/" + pointId + "/status", "{\"status\":\"" + st + "\"}");
    }

    private void expectRefused(UUID id, String code) throws Exception {
        long rev = readTour(id).get("revision").asLong();
        performStart(id, "agent1").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.issues[0].code").value(code))
                .andExpect(jsonPath("$.message").value(Matchers.startsWith("The tour cannot be started (")));
        JsonNode t = readTour(id);
        assertThat(t.get("status").asText()).isEqualTo("PLANNED");
        assertThat(t.get("revision").asLong()).isEqualTo(rev);
    }

    @Test
    void driverStartsPlannedTour() throws Exception {
        UUID id = doCreatePlanned();
        long rev = readTour(id).get("revision").asLong();
        performStart(id, "agent1").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedBy").value("agent1"))
                .andExpect(jsonPath("$.startedAt").exists())
                .andExpect(jsonPath("$.revision").value(rev + 1))
                .andExpect(jsonPath("$.route.method").value("NN_PLUS_2_OPT"))
                .andExpect(jsonPath("$.startIssues.length()").value(0))
                .andExpect(jsonPath("$.readyToStart").value(false))
                .andExpect(jsonPath("$.readyToComplete").value(false));
        JsonNode t = readTour(id);
        assertThat(t.get("startedBy").asText()).isEqualTo("agent1");
        t.get("stops").forEach(s -> assertThat(s.get("status").asText()).isEqualTo("PENDING"));
        performStart(id, "agent1").andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(ALREADY));
        assertThat(readTour(id).get("revision").asLong()).isEqualTo(rev + 1);
    }

    @Test
    void collectorMayStart() throws Exception {
        UUID id = doCreatePlanned();
        performStart(id, "agent3").andExpect(status().isOk()).andExpect(jsonPath("$.startedBy").value("agent3"));
    }

    @Test
    void nonCrewAndRolesDenied() throws Exception {
        UUID id = doCreatePlanned();
        JsonNode before = readTour(id);
        performStart(id, "agent4").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not assigned to this tour."));
        performStart(id, "agent5").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(Matchers.startsWith("Your account is not linked")));
        mvc.perform(post("/api/tours/" + id + "/start").with(asPlanner()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/tours/" + id + "/start").with(asAdmin()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/tours/" + id + "/start").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tours/" + id + "/start").with(asAgent("agent1"))).andExpect(status().isForbidden());
        performStart(UUID.randomUUID(), "agent1").andExpect(status().isNotFound());
        mvc.perform(post("/api/tours/not-a-uuid/start").with(asAgent("agent1")).with(csrf()))
                .andExpect(status().isBadRequest());
        JsonNode after = readTour(id);
        assertThat(after.get("status").asText()).isEqualTo(before.get("status").asText());
        assertThat(after.get("revision").asLong()).isEqualTo(before.get("revision").asLong());
    }

    @Test
    void onlyPlannedCanStart() throws Exception {
        UUID draft = doCreateDraft();
        performStart(draft, "agent1").andExpect(status().isForbidden());
        UUID running = doCreateInProgress();
        performStart(running, "agent1").andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(ALREADY));
    }

    @Test
    void deactivatedPointBlocksStart() throws Exception {
        UUID id = doCreatePlanned();
        doPointStatus(p2.getId(), "INACTIVE");
        performMineOne(id, "agent1").andExpect(status().isOk())
                .andExpect(jsonPath("$.startIssues.length()").value(1))
                .andExpect(jsonPath("$.startIssues[0].code").value("INACTIVE_STOP"))
                .andExpect(jsonPath("$.readyToStart").value(false));
        JsonNode t = readTour(id);
        assertThat(t.get("startIssues").get(0).get("code").asText()).isEqualTo("INACTIVE_STOP");
        expectRefused(id, "INACTIVE_STOP");
        doPointStatus(p2.getId(), "ACTIVE");
        performStart(id, "agent1").andExpect(status().isOk());
    }

    @Test
    void unavailableResourcesBlockStart() throws Exception {
        UUID id = doCreatePlanned();
        doPatch("/api/vehicles/" + big.getId() + "/status", "{\"status\":\"UNAVAILABLE\"}");
        expectRefused(id, "VEHICLE_UNAVAILABLE");
        doPatch("/api/vehicles/" + big.getId() + "/status", "{\"status\":\"AVAILABLE\"}");
        doPatch("/api/agents/" + dana.getId() + "/status", "{\"status\":\"UNAVAILABLE\"}");
        expectRefused(id, "DRIVER_UNAVAILABLE");
        doPatch("/api/agents/" + dana.getId() + "/status", "{\"status\":\"AVAILABLE\"}");
        doPatch("/api/agents/" + eli.getId() + "/status", "{\"status\":\"UNAVAILABLE\"}");
        expectRefused(id, "COLLECTOR_UNAVAILABLE");
        doPatch("/api/agents/" + eli.getId() + "/status", "{\"status\":\"AVAILABLE\"}");
        performStart(id, "agent1").andExpect(status().isOk());
    }

    private void movePoint(double lonDelta) throws Exception {
        mvc.perform(put("/api/collection-points/" + p1.getId()).with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + p1.getName() + "\",\"longitude\":" + (p1.getLongitude() + lonDelta)
                        + ",\"latitude\":" + p1.getLatitude() + "}")).andExpect(status().isOk());
    }

    @Test
    void movedPointBlocksStart() throws Exception {
        UUID id = doCreatePlanned();
        movePoint(0.001);
        long rev = readTour(id).get("revision").asLong();
        performStart(id, "agent1").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.issues[0].code").value("ROUTE_OUTDATED"))
                .andExpect(jsonPath("$.issues[0].message").value(Matchers.containsString("has moved")));
        assertThat(readTour(id).get("revision").asLong()).isEqualTo(rev);
        movePoint(0.0);
        performStart(id, "agent1").andExpect(status().isOk());
    }

    @Test
    void pastPlanningDateDoesNotBlockStart() throws Exception {
        UUID id = doCreatePlanned();
        Tour t = tours.findById(id).orElseThrow();
        t.setPlanningDate(LocalDate.now().minusDays(1));
        tours.save(t);
        performStart(id, "agent1").andExpect(status().isOk());
    }

    @Test
    void mineListsInProgressFirst() throws Exception {
        UUID planned = doCreatePlanned(LocalDate.now().plusDays(1));
        UUID running = doCreatePlanned(LocalDate.now().plusDays(2));
        performStart(running, "agent1").andExpect(status().isOk());
        performMine("agent1").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(running.toString()))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].id").value(planned.toString()));
    }
}
