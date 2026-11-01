package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Integration tests of stop outcomes recorded by the crew (S7, US-28, D-031). */
class TourStopOutcomeControllerTest extends ExecutionTestSupport {

    private String collected(UUID cat, String kg) {
        return "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\"" + cat + "\",\"actualQuantityKg\":" + kg + "}]}";
    }

    private JsonNode stop(JsonNode tour, UUID stopId) {
        for (JsonNode s : tour.get("stops")) {
            if (s.get("id").asText().equals(stopId.toString())) {
                return s;
            }
        }
        throw new AssertionError("stop");
    }

    private void expectPending(UUID tour, UUID stop) throws Exception {
        assertThat(stop(readTour(tour), stop).get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    void driverRecordsCollectedStop() throws Exception {
        UUID tour = doCreateInProgress();
        UUID s1 = stopOfPoint(tour, p1);
        long rev = readTour(tour).get("revision").asLong();
        String body = "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\"" + paper.getId()
                + "\",\"actualQuantityKg\":9.5},{\"categoryId\":\"" + glass.getId()
                + "\",\"actualQuantityKg\":0}],\"incidentReason\":\"  Lid broken \"}";
        String res = performOutcome(tour, s1, "agent1", body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode s = stop(mapper.readTree(res), s1);
        assertThat(s.get("status").asText()).isEqualTo("COLLECTED");
        assertThat(s.get("actualQuantities")).hasSize(2);
        assertThat(s.get("actualQuantities").get(0).get("categoryName").asText()).isEqualTo("Glass");
        assertThat(s.get("actualQuantities").get(0).get("actualQuantityKg").decimalValue().signum()).isZero();
        assertThat(s.get("actualQuantities").get(1).get("categoryName").asText()).isEqualTo("Paper");
        assertThat(s.get("actualQuantities").get(1).get("actualQuantityKg").asDouble()).isEqualTo(9.5);
        assertThat(s.get("actualTotalKg").asDouble()).isEqualTo(9.5);
        assertThat(s.get("incidentReason").asText()).isEqualTo("Lid broken");
        assertThat(s.get("outcomeRecordedBy").asText()).isEqualTo("agent1");
        assertThat(s.has("outcomeRecordedAt")).isTrue();
        JsonNode t = mapper.readTree(res);
        assertThat(t.get("revision").asLong()).isEqualTo(rev + 1);
        t.get("stops").forEach(o -> {
            if (!o.get("id").asText().equals(s1.toString())) {
                assertThat(o.get("status").asText()).isEqualTo("PENDING");
            }
        });
        JsonNode list = mapper.readTree(mvc.perform(get("/api/tours").with(asPlanner())).andReturn()
                .getResponse().getContentAsString());
        assertThat(list.get(0).get("terminalStopCount").asInt()).isEqualTo(1);
    }

    @Test
    void collectorRecordsNotCollectedStop() throws Exception {
        UUID tour = doCreateInProgress();
        UUID s2 = stopOfPoint(tour, p2);
        String res = performOutcome(tour, s2, "agent2",
                "{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"Blocked by a parked truck\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode s = stop(mapper.readTree(res), s2);
        assertThat(s.get("status").asText()).isEqualTo("NOT_COLLECTED");
        assertThat(s.get("actualQuantities")).isEmpty();
        assertThat(s.has("actualTotalKg")).isFalse();
        assertThat(s.get("incidentReason").asText()).isEqualTo("Blocked by a parked truck");
        assertThat(s.get("outcomeRecordedBy").asText()).isEqualTo("agent2");
    }

    @Test
    void outcomeAcceptsUnplannedActiveCategory() throws Exception {
        UUID tour = doCreateInProgress();
        performOutcome(tour, stopOfPoint(tour, p2), "agent1", collected(glass.getId(), "4")).andExpect(status().isOk());
    }

    @Test
    void validationErrors() throws Exception {
        UUID tour = doCreateInProgress();
        UUID s = stopOfPoint(tour, p1);
        String pid = paper.getId().toString();
        String long501 = "x".repeat(501);
        String[][] bad = {
            {"{}", null},
            {"{\"status\":\"PENDING\"}", "status"},
            {"{\"status\":\"COLLECTED\"}", "quantities"},
            {"{\"status\":\"COLLECTED\",\"quantities\":[]}", "quantities"},
            {collected(paper.getId(), "-1"), "quantities[0].actualQuantityKg"},
            {collected(paper.getId(), "1.234"), null},
            {"{\"status\":\"COLLECTED\",\"quantities\":[{\"actualQuantityKg\":1}]}", null},
            {"{\"status\":\"NOT_COLLECTED\"}", "incidentReason"},
            {"{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"   \"}", "incidentReason"},
            {"{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"r\",\"quantities\":[{\"categoryId\":\"" + pid
                    + "\",\"actualQuantityKg\":1}]}", "quantities"},
            {"{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"" + long501 + "\"}", null},
            {"{\"status\":", null},
        };
        for (String[] b : bad) {
            var r = performOutcome(tour, s, "agent1", b[0]).andExpect(status().isBadRequest());
            if (b[1] != null) {
                r.andExpect(jsonPath("$.fieldErrors[0].field").value(b[1]));
            }
        }
        mvc.perform(post("/api/tours/" + tour + "/stops/not-a-uuid/outcome").with(asAgent("agent1")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(collected(paper.getId(), "1")))
                .andExpect(status().isBadRequest());
        expectPending(tour, s);
    }

    @Test
    void lookupAndRuleErrors() throws Exception {
        UUID tour = doCreateInProgress();
        UUID other = doCreateInProgress();
        UUID s = stopOfPoint(tour, p1);
        performOutcome(UUID.randomUUID(), s, "agent1", collected(paper.getId(), "1")).andExpect(status().isNotFound());
        performOutcome(tour, UUID.randomUUID(), "agent1", collected(paper.getId(), "1")).andExpect(status().isNotFound());
        performOutcome(tour, stopOfPoint(other, p1), "agent1", collected(paper.getId(), "1"))
                .andExpect(status().isNotFound());
        performOutcome(tour, s, "agent1", collected(UUID.randomUUID(), "1")).andExpect(status().isNotFound());
        expectPending(tour, s);
        String twice = "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\"" + paper.getId()
                + "\",\"actualQuantityKg\":1},{\"categoryId\":\"" + paper.getId() + "\",\"actualQuantityKg\":2}]}";
        performOutcome(tour, s, "agent1", twice).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Each waste category may appear only once."));
        performOutcome(tour, s, "agent1", collected(oldCat.getId(), "1")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("is inactive")));
        expectPending(tour, s);
    }

    @Test
    void stateErrors() throws Exception {
        UUID planned = doCreatePlanned();
        performOutcome(planned, stopOfPoint(planned, p1), "agent1", collected(paper.getId(), "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.startsWith("Outcomes can be recorded only on an IN_PROGRESS tour")));
        UUID tour = doCreateInProgress();
        UUID s = stopOfPoint(tour, p1);
        performOutcome(tour, s, "agent1", collected(paper.getId(), "1")).andExpect(status().isOk());
        performOutcome(tour, s, "agent1", collected(paper.getId(), "2")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(Matchers.containsString("already has the outcome COLLECTED")));
    }

    @Test
    void crewAndRoleMatrix() throws Exception {
        UUID tour = doCreateInProgress();
        UUID s = stopOfPoint(tour, p1);
        String body = collected(paper.getId(), "1");
        performOutcome(tour, s, "agent4", body).andExpect(status().isForbidden());
        performOutcome(tour, s, "agent5", body).andExpect(status().isForbidden());
        String path = "/api/tours/" + tour + "/stops/" + s + "/outcome";
        mvc.perform(post(path).with(asPlanner()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(asAdmin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(path).with(asAgent("agent1")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        expectPending(tour, s);
    }

    @Test
    void outcomesVisibleToPlanner() throws Exception {
        UUID tour = doCreateInProgress();
        UUID s1 = stopOfPoint(tour, p1);
        UUID s2 = stopOfPoint(tour, p2);
        performOutcome(tour, s1, "agent1", collected(paper.getId(), "8.25")).andExpect(status().isOk());
        performOutcome(tour, s2, "agent2", "{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"Blocked\"}")
                .andExpect(status().isOk());
        JsonNode planner = readTour(tour);
        JsonNode agent = mapper.readTree(performMineOne(tour, "agent3").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(stop(planner, s1).get("status").asText()).isEqualTo("COLLECTED");
        assertThat(stop(planner, s2).get("status").asText()).isEqualTo("NOT_COLLECTED");
        assertThat(stop(planner, s2).get("incidentReason").asText()).isEqualTo("Blocked");
        assertThat(stop(planner, s1).get("outcomeRecordedBy").asText()).isEqualTo("agent1");
        assertThat(stop(planner, s2).get("outcomeRecordedBy").asText()).isEqualTo("agent2");
        for (UUID id : new UUID[] {s1, s2}) {
            assertThat(stop(agent, id).toString()).isEqualTo(stop(planner, id).toString());
        }
        assertThat(stop(planner, s1).get("actualQuantities").get(0).get("actualQuantityKg").asDouble()).isEqualTo(8.25);
    }
}
