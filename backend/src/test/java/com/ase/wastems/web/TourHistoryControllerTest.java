package com.ase.wastems.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ase.wastems.domain.DataOrigin;
import com.ase.wastems.domain.Vehicle;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** History of completed and cancelled tours (S8, US-32). */
class TourHistoryControllerTest extends ExecutionTestSupport {

    private List<String> doHistoryIds(String query) throws Exception {
        JsonNode rows = mapper.readTree(mvc.perform(get("/api/tours/history" + query).with(asPlanner()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        List<String> ids = new ArrayList<>();
        rows.forEach(r -> ids.add(r.get("id").asText()));
        return ids;
    }

    private ResultActions performCancel(UUID id, String reason) throws Exception {
        return mvc.perform(post("/api/tours/" + id + "/cancellation").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}"));
    }

    private String collected(String kg) {
        return "{\"status\":\"COLLECTED\",\"quantities\":[{\"categoryId\":\"" + paper.getId()
                + "\",\"actualQuantityKg\":" + kg + "}]}";
    }

    /** Starts the (planned) tour as agent1, records every stop and completes it. */
    private void doComplete(UUID id) throws Exception {
        performStart(id, "agent1").andExpect(status().isOk());
        for (UUID s : stopIds(id)) {
            performOutcome(id, s, "agent1", collected("1")).andExpect(status().isOk());
        }
        performComplete(id, "agent1").andExpect(status().isOk());
    }

    private UUID doCompletedOn(LocalDate date) throws Exception {
        UUID id = doCreatePlanned(date);
        doComplete(id);
        return id;
    }

    @Test
    void historyListsOnlyCompletedAndCancelledTours() throws Exception {
        doCreateDraft();
        doCreatePlanned();
        doCreateInProgress();
        UUID c = doCompletedOn(LocalDate.now().plusDays(1));
        UUID x = doCreateDraft();
        performCancel(x, "Dropped").andExpect(status().isOk());
        assertEquals(Set.of(c.toString(), x.toString()), Set.copyOf(doHistoryIds("")));
        JsonNode rows = mapper.readTree(mvc.perform(get("/api/tours/history").with(asPlanner()))
                .andReturn().getResponse().getContentAsString());
        for (JsonNode r : rows) {
            assertEquals(0, r.get("correctionCount").asInt());
            if (r.get("id").asText().equals(c.toString())) {
                assertEquals("COMPLETED", r.get("status").asText());
                assertEquals(true, r.has("completedAt"));
                assertEquals(false, r.has("cancelledAt"));
            } else {
                assertEquals("CANCELLED", r.get("status").asText());
                assertEquals("Dropped", r.get("cancellationReason").asText());
                assertEquals(true, r.has("cancelledAt"));
            }
        }
    }

    @Test
    void historyFiltersByStatusAndDate() throws Exception {
        LocalDate d1 = LocalDate.now().plusDays(1);
        LocalDate d2 = LocalDate.now().plusDays(2);
        LocalDate d3 = LocalDate.now().plusDays(3);
        UUID c1 = doCompletedOn(d1);
        UUID c2 = doCompletedOn(d3);
        UUID x = doCreateDraft(d2);
        performCancel(x, "Dropped").andExpect(status().isOk());
        assertEquals(List.of(c2.toString(), c1.toString()), doHistoryIds("?status=COMPLETED"));
        assertEquals(List.of(x.toString()), doHistoryIds("?status=CANCELLED"));
        assertEquals(List.of(c2.toString(), x.toString()), doHistoryIds("?from=" + d2 + "&to=" + d3));
        assertEquals(List.of(c2.toString()), doHistoryIds("?from=" + d3 + "&to=" + d3));
    }

    @Test
    void historyFiltersByVehicleAgentAndPoint() throws Exception {
        UUID t1 = doCompletedOn(LocalDate.now().plusDays(1));
        Vehicle small = vehicles.save(new Vehicle("EV-SMALL", new BigDecimal("500.00"), DataOrigin.USER_ENTERED));
        UUID t2 = doCreateDraftWith(small, gus, p3);
        performCancel(t2, "Dropped").andExpect(status().isOk());
        assertEquals(List.of(t1.toString()), doHistoryIds("?vehicleId=" + big.getId()));
        assertEquals(List.of(t1.toString()), doHistoryIds("?agentId=" + eli.getId()));
        assertEquals(List.of(t2.toString()), doHistoryIds("?agentId=" + gus.getId()));
        assertEquals(List.of(t1.toString()), doHistoryIds("?pointId=" + p1.getId()));
        assertEquals(Set.of(t1.toString(), t2.toString()), Set.copyOf(doHistoryIds("?pointId=" + p3.getId())));
        assertEquals(List.of(), doHistoryIds("?agentId=" + UUID.randomUUID()));
    }

    private UUID doCreateDraftWith(Vehicle v, com.ase.wastems.domain.Agent driver,
            com.ase.wastems.domain.CollectionPoint point) throws Exception {
        String res = mvc.perform(post("/api/tours").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planningDate\":\"" + LocalDate.now().plusDays(1) + "\",\"depotId\":\"" + mn01g.getId() + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(mapper.readTree(res).get("id").asText());
        mvc.perform(post("/api/tours/" + id + "/stops").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"collectionPointId\":\"" + point.getId() + "\"}")).andExpect(status().isCreated());
        mvc.perform(put("/api/tours/" + id + "/assignment").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehicleId\":\"" + v.getId() + "\",\"driverId\":\"" + driver.getId()
                        + "\",\"collectorIds\":[]}")).andExpect(status().isOk());
        return id;
    }

    @Test
    void historyOrderNewestFirst() throws Exception {
        LocalDate d = LocalDate.now().plusDays(1);
        UUID first = doCreatePlanned(d);
        UUID second = doCreatePlanned(d);
        doComplete(first);
        Thread.sleep(20);
        doComplete(second);
        UUID later = doCompletedOn(d.plusDays(2));
        assertEquals(List.of(later.toString(), second.toString(), first.toString()), doHistoryIds(""));
    }

    @Test
    void historyValidation() throws Exception {
        for (String q : new String[] {"?status=PLANNED", "?status=NOPE", "?from=2026-12-02&to=2026-12-01",
                "?from=12/01/2026", "?vehicleId=abc"}) {
            mvc.perform(get("/api/tours/history" + q).with(asPlanner())).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/tours/history?status=PLANNED").with(asPlanner()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
        mvc.perform(get("/api/tours/history?from=2026-12-02&to=2026-12-01").with(asPlanner()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("to"));
    }

    @Test
    void historyRoles() throws Exception {
        mvc.perform(get("/api/tours/history").with(asAgent("agent1"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/tours/history")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tours/history").with(asAdmin())).andExpect(status().isOk());
    }

    @Test
    void historyDetailRetainsTheRecordAndCannotBeReopened() throws Exception {
        UUID done = doCreatePlanned();
        performStart(done, "agent1").andExpect(status().isOk());
        for (UUID s : stopIds(done)) {
            performOutcome(done, s, "agent1", collected("2")).andExpect(status().isOk());
        }
        performComplete(done, "agent1").andExpect(status().isOk());
        UUID a = stopOfPoint(done, p1);
        mvc.perform(post("/api/tours/" + done + "/stops/" + a + "/corrections").with(asPlanner()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"NOT_COLLECTED\",\"incidentReason\":\"Missing\",\"reason\":\"Checked\"}"))
                .andExpect(status().isOk());
        UUID running = doCreateInProgress();
        UUID r1 = stopOfPoint(running, p1);
        performOutcome(running, r1, "agent1", collected("3")).andExpect(status().isOk());
        performCancel(running, "Snow").andExpect(status().isOk());

        JsonNode d = readTour(done);
        assertEquals("COMPLETED", d.get("status").asText());
        assertEquals(true, d.has("vehicle") && d.has("driver") && d.has("startedAt") && d.has("completedAt"));
        assertEquals(2, d.get("collectors").size());
        assertEquals(3, d.get("stops").size());
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, d.get("stops").get(i).get("visitOrder").asInt());
            assertEquals(true, d.get("stops").get(i).has("outcomeRecordedAt"));
        }
        boolean sawCorrection = false;
        for (JsonNode s : d.get("stops")) {
            sawCorrection |= s.get("corrections").size() == 1 && s.get("status").asText().equals("NOT_COLLECTED")
                    && s.get("incidentReason").asText().equals("Missing");
            if (s.get("status").asText().equals("COLLECTED")) {
                assertEquals(1, s.get("actualQuantities").size());
            }
        }
        assertEquals(true, sawCorrection);
        JsonNode c = readTour(running);
        assertEquals("CANCELLED", c.get("status").asText());
        assertEquals("Snow", c.get("cancellationReason").asText());
        assertEquals("IN_PROGRESS", c.get("cancelledFromStatus").asText());
        assertEquals(true, c.has("cancelledAt") && c.has("startedAt"));

        mvc.perform(delete("/api/tours/" + done).with(asPlanner()).with(csrf())).andExpect(status().isMethodNotAllowed());
        mvc.perform(put("/api/tours/" + done).with(asPlanner()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"planningDate\":\"" + LocalDate.now().plusDays(5) + "\",\"depotId\":\"" + mn01g.getId() + "\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/tours/" + done + "/confirmation").with(asPlanner()).with(csrf()))
                .andExpect(status().isConflict());
        performStart(done, "agent1").andExpect(status().isConflict());
        assertEquals(Set.of(done.toString(), running.toString()), Set.copyOf(doHistoryIds("")));
    }
}
