package com.ase.wastems.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.Agent;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Crew-only access to assigned tours (S7, US-26, US-03, US-16). */
class TourAssignedAccessControllerTest extends ExecutionTestSupport {

    private static final String NOT_ASSIGNED = "You are not assigned to this tour.";
    private static final String NOT_LINKED = "Your account is not linked to an operational agent.";

    @Test
    void crewSeesPlannedTourInMine() throws Exception {
        UUID id = doCreatePlanned();
        for (String u : List.of("agent1", "agent2", "agent3")) {
            performMine(u).andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(id.toString()))
                    .andExpect(jsonPath("$[0].status").value("PLANNED"))
                    .andExpect(jsonPath("$[0].origin").value("USER_ENTERED"))
                    .andExpect(jsonPath("$[0].stopCount").value(3))
                    .andExpect(jsonPath("$[0].terminalStopCount").value(0));
        }
    }

    @Test
    void draftIsNotVisibleToItsCrew() throws Exception {
        UUID draft = doCreateDraft();
        performMine("agent1").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        performMineOne(draft, "agent1").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(NOT_ASSIGNED));
    }

    @Test
    void otherAgentsSeeNothing() throws Exception {
        UUID id = doCreatePlanned();
        performMine("agent4").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        performMineOne(id, "agent4").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(NOT_ASSIGNED));
    }

    @Test
    void unlinkedAccountHasNoExecutionAccess() throws Exception {
        UUID id = doCreatePlanned();
        performMine("agent5").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        performMineOne(id, "agent5").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(NOT_LINKED));
        Agent f = agents.findById(finn.getId()).orElseThrow();
        f.unlinkUserAccount();
        agents.save(f);
        performMine("agent3").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        performMineOne(id, "agent3").andExpect(status().isForbidden());
    }

    @Test
    void detailCarriesEssentialsWithoutMap() throws Exception {
        UUID id = doCreatePlanned();
        performMineOne(id, "agent1").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.planningDate").exists())
                .andExpect(jsonPath("$.depot.name").value("MN01G"))
                .andExpect(jsonPath("$.stops.length()").value(3))
                .andExpect(jsonPath("$.stops[0].visitOrder").value(1))
                .andExpect(jsonPath("$.stops[1].visitOrder").value(2))
                .andExpect(jsonPath("$.stops[2].visitOrder").value(3))
                .andExpect(jsonPath("$.stops[*].name").exists())
                .andExpect(jsonPath("$.stops[*].longitude").exists())
                .andExpect(jsonPath("$.stops[*].latitude").exists())
                .andExpect(jsonPath("$.stops[*].plannedTotalKg").exists())
                .andExpect(jsonPath("$.stops[0].actualQuantities.length()").value(0))
                .andExpect(jsonPath("$.stops[0].outcomeRecordedBy").doesNotExist())
                .andExpect(jsonPath("$.route.method").value("NN_PLUS_2_OPT"))
                .andExpect(jsonPath("$.route.legs.features.length()").value(4))
                .andExpect(jsonPath("$.driver.id").value(dana.getId().toString()))
                .andExpect(jsonPath("$.collectors.length()").value(2))
                .andExpect(jsonPath("$.vehicle.registration").value("EV-BIG"))
                .andExpect(jsonPath("$.startIssues.length()").value(0))
                .andExpect(jsonPath("$.readyToStart").value(true))
                .andExpect(jsonPath("$.readyToComplete").value(false))
                .andExpect(jsonPath("$.startedAt").doesNotExist());
        JsonNode t = mapper.readTree(performMineOne(id, "agent1").andReturn().getResponse().getContentAsString());
        for (JsonNode s : t.get("stops")) {
            assertThat(s.get("status").asText()).isEqualTo("PENDING");
            int demands = s.get("plannedDemands").size();
            assertThat(demands).isEqualTo(s.get("collectionPointId").asText().equals(p1.getId().toString()) ? 2 : 1);
        }
    }

    @Test
    void roleAndAuthenticationMatrix() throws Exception {
        UUID id = doCreatePlanned();
        mvc.perform(get("/api/tours/mine").with(asPlanner())).andExpect(status().isForbidden());
        mvc.perform(get("/api/tours/mine").with(asAdmin())).andExpect(status().isForbidden());
        mvc.perform(get("/api/tours/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tours/" + id).with(asAgent("agent1"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/collection-points").with(asAgent("agent1"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/tours/mine/not-a-uuid").with(asAgent("agent1"))).andExpect(status().isBadRequest());
        performMineOne(UUID.randomUUID(), "agent1").andExpect(status().isNotFound());
        mvc.perform(get("/api/tours/mine?status=NOPE").with(asAgent("agent1"))).andExpect(status().isBadRequest());
    }

    @Test
    void mineOrdersByDateAndFiltersByStatus() throws Exception {
        UUID later = doCreatePlanned(LocalDate.now().plusDays(2));
        UUID sooner = doCreatePlanned(LocalDate.now().plusDays(1));
        performMine("agent1").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sooner.toString()))
                .andExpect(jsonPath("$[1].id").value(later.toString()));
        mvc.perform(get("/api/tours/mine?status=PLANNED").with(asAgent("agent1")))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/tours/mine?status=COMPLETED").with(asAgent("agent1")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void plannerResponsesCarryExecutionFields() throws Exception {
        UUID planned = doCreatePlanned();
        UUID draft = doCreateDraft();
        JsonNode t = readTour(planned);
        assertThat(t.get("startIssues")).isEmpty();
        assertThat(t.get("readyToStart").asBoolean()).isTrue();
        assertThat(t.get("readyToComplete").asBoolean()).isFalse();
        JsonNode d = readTour(draft);
        assertThat(d.get("startIssues")).isEmpty();
        assertThat(d.get("readyToStart").asBoolean()).isFalse();
        JsonNode list = mapper.readTree(mvc.perform(get("/api/tours").with(asPlanner()))
                .andReturn().getResponse().getContentAsString());
        boolean found = false;
        for (JsonNode row : list) {
            if (row.get("id").asText().equals(planned.toString())) {
                found = true;
                assertThat(row.get("terminalStopCount").asInt()).isZero();
                assertThat(row.get("origin").asText()).isEqualTo("USER_ENTERED");
            }
        }
        assertThat(found).isTrue();
    }
}
