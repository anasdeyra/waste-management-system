package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.CollectionPoint;
import com.ase.wastems.domain.CollectionPointStatus;
import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.ResourceStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class DashboardControllerTest extends ExecutionTestSupport {

    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUpDashboard() throws Exception {
        gus.setStatus(ResourceStatus.UNAVAILABLE);
        agents.save(gus);
        CollectionPoint inactive = new CollectionPoint("Inactive test", -74.001, 40.715, DataOrigin.USER_ENTERED);
        inactive.setStatus(CollectionPointStatus.INACTIVE);
        points.save(inactive);
        doCreateDraft();
        doCreatePlanned();
        UUID running = doCreateInProgress();
        performOutcome(running, stopOfPoint(running, p1), "agent1",
                "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\"" + paper.getId()
                        + "\",\"actualQuantityKg\":7.5}]}").andExpect(status().isOk());
        performOutcome(running, stopOfPoint(running, p2), "agent1",
                "{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"Blocked\"}").andExpect(status().isOk());
        UUID cancelled = doCreateDraft(tomorrow.plusDays(3));
        mvc.perform(post("/api/tours/" + cancelled + "/cancellation").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Test\"}")).andExpect(status().isOk());
    }

    private ResultActions performDashboard(String query) throws Exception {
        return mvc.perform(get("/api/dashboard/operations" + query).with(asPlanner()));
    }

    @Test
    void allDates() throws Exception {
        String body = performDashboard("").andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicles.total").value(1)).andExpect(jsonPath("$.vehicles.available").value(1))
                .andExpect(jsonPath("$.vehicles.unavailable").value(0))
                .andExpect(jsonPath("$.agents.total").value(4)).andExpect(jsonPath("$.agents.available").value(3))
                .andExpect(jsonPath("$.agents.unavailable").value(1))
                .andExpect(jsonPath("$.collectionPoints.total").value(4))
                .andExpect(jsonPath("$.collectionPoints.active").value(3))
                .andExpect(jsonPath("$.collectionPoints.inactive").value(1))
                .andExpect(jsonPath("$.tours.total").value(4)).andExpect(jsonPath("$.tours.draft").value(1))
                .andExpect(jsonPath("$.tours.planned").value(1)).andExpect(jsonPath("$.tours.inProgress").value(1))
                .andExpect(jsonPath("$.tours.completed").value(0)).andExpect(jsonPath("$.tours.cancelled").value(1))
                .andExpect(jsonPath("$.stops.total").value(6)).andExpect(jsonPath("$.stops.pending").value(4))
                .andExpect(jsonPath("$.stops.collected").value(1)).andExpect(jsonPath("$.stops.notCollected").value(1))
                .andExpect(jsonPath("$.from").doesNotExist()).andExpect(jsonPath("$.to").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(body).get("collectedKg").asDouble()).isEqualTo(7.5);
    }

    @Test
    void periodFilters() throws Exception {
        performDashboard("?from=" + tomorrow + "&to=" + tomorrow).andExpect(status().isOk())
                .andExpect(jsonPath("$.tours.total").value(3)).andExpect(jsonPath("$.tours.cancelled").value(0))
                .andExpect(jsonPath("$.stops.total").value(6))
                .andExpect(jsonPath("$.from").value(tomorrow.toString()))
                .andExpect(jsonPath("$.to").value(tomorrow.toString()));
        String later = performDashboard("?from=" + tomorrow.plusDays(3)).andExpect(status().isOk())
                .andExpect(jsonPath("$.tours.total").value(1)).andExpect(jsonPath("$.tours.cancelled").value(1))
                .andExpect(jsonPath("$.stops.total").value(0)).andReturn().getResponse().getContentAsString();
        JsonNode n = mapper.readTree(later);
        assertThat(n.get("collectedKg").asDouble()).isEqualTo(0.0);
    }

    @Test
    void invalidPeriods() throws Exception {
        performDashboard("?from=" + tomorrow.plusDays(3) + "&to=" + tomorrow).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("to"));
        performDashboard("?from=bad").andExpect(status().isBadRequest());
    }

    @Test
    void roles() throws Exception {
        mvc.perform(get("/api/dashboard/operations").with(asAdmin())).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/operations").with(asAgent("agent1"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard/operations")).andExpect(status().isUnauthorized());
    }
}
