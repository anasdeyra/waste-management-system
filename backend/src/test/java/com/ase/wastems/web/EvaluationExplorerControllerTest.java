package com.ase.wastems.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.ase.wastems.domain.EvaluationBatch;
import com.ase.wastems.service.DemandProfile;
import com.ase.wastems.service.ExperimentService;
import com.ase.wastems.service.ExperimentSpec;
import com.ase.wastems.service.ImportTrigger;
import com.ase.wastems.service.Mn01ImportService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class EvaluationExplorerControllerTest extends ExecutionTestSupport {

    @Autowired Mn01ImportService mn01Import;
    @Autowired ExperimentService experiments;
    @Autowired JdbcTemplate jdbc;

    private UUID batchId;
    private String tourRunId;

    @BeforeEach
    void setUpExplorer() throws Exception {
        jdbc.update("delete from evaluation_batch");
        mn01Import.importAll(ImportTrigger.MANUAL);
        UUID tour = doCreateDraft();
        tourRunId = mapper.readTree(mvc.perform(post("/api/tours/" + tour + "/evaluations").with(asPlanner()).with(csrf()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
        EvaluationBatch b = experiments.run(ExperimentSpec.of(List.of(DemandProfile.UNIFORM_LOW, DemandProfile.UNIFORM_HIGH),
                List.of(4L), List.of(5, 8), null, null), "admin1");
        batchId = b.getId();
    }

    @AfterEach
    void cleanExplorer() {
        jdbc.update("delete from evaluation_batch");
        sourceSnapshots.deleteAll();
    }

    private ResultActions performList(String query) throws Exception {
        return mvc.perform(get("/api/evaluations" + query).with(asPlanner()));
    }

    @Test
    void filtersByKind() throws Exception {
        performList("?kind=EXPERIMENT").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.items[0].scenarioKey").exists())
                .andExpect(jsonPath("$.items[0].batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.items[0].pointCount").exists())
                .andExpect(jsonPath("$.items[0].results.length()").value(3))
                .andExpect(jsonPath("$.items[0].results[1].distanceChangePct").exists())
                .andExpect(jsonPath("$.items[0].results[0].legs").doesNotExist())
                .andExpect(jsonPath("$.items[0].results[0].visitOrder").doesNotExist());
        performList("?kind=TOUR_DRAFT").andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].tourId").exists())
                .andExpect(jsonPath("$.items[0].batchId").doesNotExist());
    }

    @Test
    void combinesFiltersAndPages() throws Exception {
        performList("?batchId=" + batchId + "&pointCount=8").andExpect(jsonPath("$.totalElements").value(2));
        performList("?profile=UNIFORM_HIGH").andExpect(jsonPath("$.totalElements").value(2));
        performList("?seed=4").andExpect(jsonPath("$.totalElements").value(4));
        performList("?status=ROUTING_FAILED").andExpect(jsonPath("$.totalElements").value(0));
        performList("?kind=EXPERIMENT&page=1&size=3").andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void invalidParametersAre400() throws Exception {
        performList("?page=-1").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("page"));
        performList("?size=0").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
        performList("?size=201").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
        performList("?pointCount=0").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("pointCount"));
        performList("?kind=NOPE").andExpect(status().isBadRequest());
        performList("?batchId=x").andExpect(status().isBadRequest());
    }

    @Test
    void batchesAreListedNewestFirst() throws Exception {
        mvc.perform(get("/api/evaluations/batches").with(asPlanner())).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(batchId.toString()))
                .andExpect(jsonPath("$[0].kind").value("EXPERIMENT"))
                .andExpect(jsonPath("$[0].canonical").value(false))
                .andExpect(jsonPath("$[0].demandProfiles[0]").value("UNIFORM_LOW"))
                .andExpect(jsonPath("$[0].demandProfiles[1]").value("UNIFORM_HIGH"))
                .andExpect(jsonPath("$[0].seeds[0]").value(4))
                .andExpect(jsonPath("$[0].sizes[0]").value(5))
                .andExpect(jsonPath("$[0].sizes[1]").value(8));
    }

    @Test
    void batchDetailHasAggregatesAndKeys() throws Exception {
        mvc.perform(get("/api/evaluations/batches/" + batchId).with(asPlanner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.batch.runCount").value(4))
                .andExpect(jsonPath("$.scenarioKeys[0]").value("mn01-UNIFORM_HIGH-s4-n5"))
                .andExpect(jsonPath("$.scenarioKeys.length()").value(4))
                .andExpect(jsonPath("$.aggregates.length()").value(15))
                .andExpect(jsonPath("$.aggregates[0].group").value("ALL"))
                .andExpect(jsonPath("$.aggregates[0].value").value("all"))
                .andExpect(jsonPath("$.aggregates[0].method").value("INPUT_ORDER"))
                .andExpect(jsonPath("$.aggregates[0].runs").value(4))
                .andExpect(jsonPath("$.aggregates[0].meanDistanceChangePct").value(0.0));
    }

    @Test
    void runDetailCarriesScenarioFieldsAndChanges() throws Exception {
        String id = performList("?kind=EXPERIMENT&seed=4&pointCount=5&profile=UNIFORM_LOW").andReturn().getResponse()
                .getContentAsString();
        String runId = mapper.readTree(id).get("items").get(0).get("id").asText();
        JsonNode run = mapper.readTree(mvc.perform(get("/api/evaluations/" + runId).with(asPlanner()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.batchId").value(batchId.toString()))
                .andExpect(jsonPath("$.scenarioKey").value("mn01-UNIFORM_LOW-s4-n5"))
                .andExpect(jsonPath("$.demandProfile").value("UNIFORM_LOW"))
                .andExpect(jsonPath("$.seed").value(4))
                .andExpect(jsonPath("$.repetition").value(1))
                .andExpect(jsonPath("$.results[0].distanceChangePct").value(0.0))
                .andReturn().getResponse().getContentAsString());
        assertThat(run.get("stops").get(0).get("demandBreakdown").asText()).startsWith("Mixed litter=");
        double d0 = run.get("results").get(0).get("distanceM").asDouble();
        double d2 = run.get("results").get(2).get("distanceM").asDouble();
        assertThat(run.get("results").get(2).get("distanceChangePct").asDouble())
                .isCloseTo((d2 - d0) / d0 * 100, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void tourRunDetailHasNoBatchFields() throws Exception {
        mvc.perform(get("/api/evaluations/" + tourRunId).with(asPlanner())).andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").doesNotExist())
                .andExpect(jsonPath("$.scenarioKey").doesNotExist());
    }

    @Test
    void rolesAndLookups() throws Exception {
        for (String url : List.of("/api/evaluations", "/api/evaluations/batches", "/api/evaluations/batches/" + batchId)) {
            mvc.perform(get(url).with(asAgent("agent1"))).andExpect(status().isForbidden());
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/evaluations/batches/" + UUID.randomUUID()).with(asPlanner()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/evaluations/batches/not-a-uuid").with(asPlanner())).andExpect(status().isBadRequest());
    }
}
