package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Integration tests of the completion rule and the frozen record (S7, US-29). */
class TourCompletionControllerTest extends ExecutionTestSupport {

    private String paperCollected(String kg) {
        return "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\"" + paper.getId()
                + "\",\"actualQuantityKg\":" + kg + "}]}";
    }

    private void doResolveAll(UUID tour) throws Exception {
        for (UUID s : stopIds(tour)) {
            JsonNode t = readTour(tour);
            for (JsonNode st : t.get("stops")) {
                if (st.get("id").asText().equals(s.toString()) && st.get("status").asText().equals("PENDING")) {
                    performOutcome(tour, s, "agent1", paperCollected("10")).andExpect(status().isOk());
                }
            }
        }
    }

    @Test
    void incompleteTourCannotComplete() throws Exception {
        UUID tour = doCreateInProgress();
        performOutcome(tour, stopOfPoint(tour, p1), "agent1", paperCollected("10")).andExpect(status().isOk());
        long rev = readTour(tour).get("revision").asLong();
        performComplete(tour, "agent1").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.issues.length()").value(2))
                .andExpect(jsonPath("$.issues[0].code").value("STOP_PENDING"))
                .andExpect(jsonPath("$.issues[1].code").value("STOP_PENDING"))
                .andExpect(jsonPath("$.issues[0].message").value(Matchers.containsString("has no outcome yet")))
                .andExpect(jsonPath("$.message").value(Matchers.startsWith("The tour cannot be completed (")));
        JsonNode t = readTour(tour);
        assertThat(t.get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(t.get("revision").asLong()).isEqualTo(rev);
        assertThat(t.get("readyToComplete").asBoolean()).isFalse();
    }

    @Test
    void crewCompletesResolvedTour() throws Exception {
        UUID tour = doCreateInProgress();
        doResolveAll(tour);
        assertThat(readTour(tour).get("readyToComplete").asBoolean()).isTrue();
        JsonNode before = mapper.readTree(performMineOne(tour, "agent2").andReturn().getResponse().getContentAsString());
        String res = performComplete(tour, "agent2").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedBy").value("agent2"))
                .andExpect(jsonPath("$.completedAt").exists())
                .andExpect(jsonPath("$.revision").value(before.get("revision").asLong() + 1))
                .andExpect(jsonPath("$.readyToComplete").value(false))
                .andReturn().getResponse().getContentAsString();
        JsonNode after = mapper.readTree(res);
        assertThat(after.get("route").get("method")).isEqualTo(before.get("route").get("method"));
        assertThat(after.get("route").get("distanceM")).isEqualTo(before.get("route").get("distanceM"));
        assertThat(after.get("route").get("legs").get("features")).hasSameSizeAs(before.get("route").get("legs").get("features"));
        assertThat(after.get("vehicle")).isEqualTo(before.get("vehicle"));
        assertThat(after.get("driver")).isEqualTo(before.get("driver"));
        assertThat(after.get("collectors")).isEqualTo(before.get("collectors"));
        assertThat(after.get("stops")).isEqualTo(before.get("stops"));
        assertThat(after.get("startedBy")).isEqualTo(before.get("startedBy"));
        assertThat(after.get("startedAt")).isEqualTo(before.get("startedAt"));
    }

    @Test
    void completedTourIsFrozen() throws Exception {
        UUID tour = doCreateInProgress();
        doResolveAll(tour);
        performComplete(tour, "agent1").andExpect(status().isOk());
        JsonNode before = readTour(tour);
        List<UUID> stops = stopIds(tour);
        performOutcome(tour, stops.get(0), "agent1", paperCollected("1")).andExpect(status().isConflict());
        performStart(tour, "agent1").andExpect(status().isConflict());
        performComplete(tour, "agent1").andExpect(status().isConflict());
        mvc.perform(post("/api/tours/" + tour + "/stops").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"collectionPointId\":\"" + p1.getId() + "\"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/tours/" + tour + "/stops/order").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("stopIds", stops.reversed()))))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/tours/" + tour + "/assignment").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehicleId\":\"" + big.getId() + "\",\"driverId\":\"" + dana.getId()
                        + "\",\"collectorIds\":[]}")).andExpect(status().isConflict());
        mvc.perform(put("/api/tours/" + tour).with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planningDate\":\"" + java.time.LocalDate.now().plusDays(3) + "\",\"depotId\":\""
                        + mn01g.getId() + "\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/tours/" + tour + "/evaluations").with(asPlanner()).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/tours/" + tour + "/confirmation").with(asPlanner()).with(csrf()))
                .andExpect(status().isConflict());
        assertThat(readTour(tour)).isEqualTo(before);
    }

    @Test
    void completionRoleMatrix() throws Exception {
        UUID tour = doCreateInProgress();
        doResolveAll(tour);
        performComplete(tour, "agent4").andExpect(status().isForbidden());
        performComplete(tour, "agent5").andExpect(status().isForbidden());
        mvc.perform(post("/api/tours/" + tour + "/completion").with(asPlanner()).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tours/" + tour + "/completion").with(asAdmin()).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tours/" + tour + "/completion").with(csrf())).andExpect(status().isUnauthorized());
        performComplete(UUID.randomUUID(), "agent1").andExpect(status().isNotFound());
        mvc.perform(post("/api/tours/not-a-uuid/completion").with(asAgent("agent1")).with(csrf()))
                .andExpect(status().isBadRequest());
        performComplete(doCreatePlanned(), "agent1").andExpect(status().isConflict());
        performComplete(doCreateDraft(), "agent1").andExpect(status().isForbidden());
        assertThat(readTour(tour).get("status").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void completedTourStaysInMine() throws Exception {
        UUID tour = doCreateInProgress();
        doResolveAll(tour);
        performComplete(tour, "agent1").andExpect(status().isOk());
        mvc.perform(get("/api/tours/mine?status=COMPLETED").with(asAgent("agent1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(tour.toString()))
                .andExpect(jsonPath("$[0].terminalStopCount").value(3));
        performMineOne(tour, "agent1").andExpect(status().isOk());
    }
}
