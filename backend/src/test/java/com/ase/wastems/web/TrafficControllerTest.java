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

class TrafficControllerTest extends ExecutionTestSupport {

    @Test
    void overlayIsServedToPlanners() throws Exception {
        mvc.perform(get("/api/traffic/overlay").with(asPlanner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.snapshotId").value("nyc-dot-i4gi-tjb9:12:2a1b9f1faa7f"))
                .andExpect(jsonPath("$.counts.total").value(12)).andExpect(jsonPath("$.counts.observed").value(8))
                .andExpect(jsonPath("$.links.length()").value(12))
                .andExpect(jsonPath("$.links[*].geometry.type", Matchers.everyItem(Matchers.is("LineString"))))
                .andExpect(jsonPath("$.coverageNote").exists()).andExpect(jsonPath("$.displayOnlyNote").exists())
                .andExpect(jsonPath("$.attribution").exists())
                .andExpect(jsonPath("$.disclaimer",
                        Matchers.startsWith("This application may contain and/or utilize information")));
        mvc.perform(get("/api/traffic/overlay").with(asAdmin())).andExpect(status().isOk());
        mvc.perform(get("/api/traffic/overlay").with(asAgent("agent1"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/traffic/overlay")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/traffic/overlay").with(asPlanner()).with(csrf())).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void readingTheOverlayDoesNotChangeOptimization() throws Exception {
        UUID tour = doCreateDraft();
        long revision = readTour(tour).get("revision").asLong();
        JsonNode a = evaluate(tour);
        JsonNode matrixA = matrix(a.get("id").asText());
        mvc.perform(get("/api/traffic/overlay").with(asPlanner())).andExpect(status().isOk());
        mvc.perform(get("/api/traffic/overlay").with(asPlanner())).andExpect(status().isOk());
        assertThat(readTour(tour).get("revision").asLong()).isEqualTo(revision);
        JsonNode b = evaluate(tour);
        JsonNode matrixB = matrix(b.get("id").asText());
        assertThat(matrixB.get("distanceM")).isEqualTo(matrixA.get("distanceM"));
        assertThat(matrixB.get("travelTimeS")).isEqualTo(matrixA.get("travelTimeS"));
        assertThat(b.get("roadGraphVersion").asText()).isEqualTo(a.get("roadGraphVersion").asText());
    }

    private JsonNode evaluate(UUID tour) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/tours/" + tour + "/evaluations").with(asPlanner()).with(csrf()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode matrix(String runId) throws Exception {
        return mapper.readTree(mvc.perform(get("/api/evaluations/" + runId + "/matrix").with(asPlanner()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
}
