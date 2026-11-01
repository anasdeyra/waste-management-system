package com.ase.wastems.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ase.wastems.domain.EvaluationRun;
import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.domain.MethodResult;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.repository.EvaluationRunRepository;
import com.ase.wastems.routing.DirectedCostMatrixProvider;
import com.ase.wastems.routing.RoadLeg;
import com.ase.wastems.routing.RoadLegUnavailableException;
import com.ase.wastems.routing.RoadMatrix;
import com.ase.wastems.routing.RoutingUnavailableException;
import com.ase.wastems.routing.WgsPoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class EvaluationServiceTest {

    static final double[][] C = {{0, 5, 2, 3, 1}, {2, 0, 5, 1, 5}, {8, 8, 0, 3, 2}, {5, 1, 2, 0, 5},
            {3, 2, 3, 3, 0}};

    @MockitoBean
    DirectedCostMatrixProvider matrices;

    @Autowired
    EvaluationService service;

    @Autowired
    EvaluationRunRepository runs;

    @Autowired
    ObjectMapper mapper;

    private final UUID tourId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        runs.deleteAll();
        when(matrices.roadGraphVersion()).thenReturn("test-graph-v1");
        when(matrices.routingEngine()).thenReturn("test-engine");
    }

    @AfterEach
    void tearDown() {
        runs.deleteAll();
    }

    private static WgsPoint point(int k) {
        return k == 0 ? new WgsPoint(-74.0104, 40.7255) : new WgsPoint(-74.000 - k / 1000.0, 40.71);
    }

    private static RoadMatrix matrix() {
        RoadLeg[][] legs = new RoadLeg[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                legs[i][j] = new RoadLeg(C[i][j], C[i][j], List.of(point(i), point(j)));
            }
        }
        return new RoadMatrix("test-graph-v1", C, C, legs);
    }

    private FrozenScenario scenario(String capacity) {
        List<FrozenStop> stops = new ArrayList<>();
        for (int k = 1; k <= 4; k++) {
            stops.add(new FrozenStop(k, UUID.randomUUID(), "8znf-7b2c", "id" + k, "S" + k, point(k).longitude(),
                    point(k).latitude(), new BigDecimal("1.00")));
        }
        return new FrozenScenario(tourId, 7, new FrozenDepot("MN01G", -74.0104, 40.7255, "MN01G"), stops,
                new BigDecimal(capacity), List.of("8znf-7b2c:449:a0737237e370", "xw3j-2yxf:1:207851c2e229"));
    }

    @Test
    void completedRunRetainsEverything() throws Exception {
        when(matrices.buildMatrix(anyList())).thenReturn(matrix());
        FrozenScenario s = scenario("10.00");
        EvaluationRun saved = service.evaluate(s, "planner");
        EvaluationRun run = service.get(saved.getId());

        assertThat(run.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
        assertThat(run.getTourId()).isEqualTo(tourId);
        assertThat(run.getTourRevision()).isEqualTo(7L);
        assertThat(run.getCreatedBy()).isEqualTo("planner");
        assertThat(run.getRoadGraphVersion()).isEqualTo("test-graph-v1");
        assertThat(run.getRoutingEngine()).isEqualTo("test-engine");
        assertThat(run.sourceSnapshotIdList()).containsExactly("8znf-7b2c:449:a0737237e370",
                "xw3j-2yxf:1:207851c2e229");
        assertThat(run.getPointCount()).isEqualTo(4);
        assertThat(run.getTotalDemandKg()).isEqualByComparingTo("4.00");
        assertThat(run.getServiceTimeSeconds()).isEqualTo(600.0);
        assertThat(run.getDepotName()).isEqualTo("MN01G");
        assertThat(run.getDepotLongitude()).isEqualTo(-74.0104);
        assertThat(run.getDepotLatitude()).isEqualTo(40.7255);
        assertThat(run.getDepotSourceIdentifier()).isEqualTo("MN01G");
        for (int k = 0; k < 4; k++) {
            assertThat(run.getStops().get(k).getInputIndex()).isEqualTo(k + 1);
            assertThat(run.getStops().get(k).getName()).isEqualTo("S" + (k + 1));
            assertThat(run.getStops().get(k).getLongitude()).isEqualTo(point(k + 1).longitude());
            assertThat(run.getStops().get(k).getLatitude()).isEqualTo(point(k + 1).latitude());
        }

        List<MethodResult> r = run.getResults();
        assertThat(r).extracting(MethodResult::getMethod).containsExactly(PlanningMethod.INPUT_ORDER,
                PlanningMethod.NEAREST_NEIGHBOR, PlanningMethod.NN_PLUS_2_OPT);
        assertThat(r.get(0).visitOrderList()).containsExactly(1, 2, 3, 4);
        assertThat(r.get(1).visitOrderList()).containsExactly(4, 1, 3, 2);
        assertThat(r.get(2).visitOrderList()).containsExactly(4, 2, 3, 1);
        double[] dist = {21, 14, 10};
        double[] dur = {2421, 2414, 2410};
        for (int i = 0; i < 3; i++) {
            assertThat(r.get(i).getDistanceM()).isCloseTo(dist[i], within(1e-9));
            assertThat(r.get(i).getDurationS()).isCloseTo(dur[i], within(1e-9));
            assertThat(r.get(i).isFeasible()).isTrue();
            JsonNode geo = mapper.readTree(r.get(i).getLegsGeoJson());
            assertThat(geo.get("type").asText()).isEqualTo("FeatureCollection");
            assertThat(geo.get("features")).hasSize(5);
            double sum = 0;
            for (JsonNode f : geo.get("features")) {
                sum += f.get("properties").get("distanceM").asDouble();
            }
            assertThat(sum).isCloseTo(dist[i], within(1e-9));
            assertThat(geo.get("features").get(0).get("properties").get("fromIndex").asInt()).isZero();
        }
        JsonNode mx = mapper.readTree(run.getMatrixJson());
        assertThat(mx.get("nodeCount").asInt()).isEqualTo(5);
        assertThat(mx.get("distanceM").get(0).get(4).asDouble()).isEqualTo(1.0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<WgsPoint>> cap = ArgumentCaptor.forClass(List.class);
        verify(matrices).buildMatrix(cap.capture());
        assertThat(cap.getValue()).hasSize(5);
        assertThat(cap.getValue().get(0)).isEqualTo(new WgsPoint(-74.0104, 40.7255));
    }

    @Test
    void missingLegIsRetainedAsRoutingFailure() {
        when(matrices.buildMatrix(anyList())).thenThrow(new RoadLegUnavailableException(
                RoadLegUnavailableException.Reason.NO_ROAD_PATH, 2, 1, "connection not found"));
        EvaluationRun run = service.get(service.evaluate(scenario("10.00"), "planner").getId());
        assertThat(run.getStatus()).isEqualTo(EvaluationStatus.ROUTING_FAILED);
        assertThat(run.getFailureCode()).isEqualTo("NO_ROAD_PATH");
        assertThat(run.getFailureMessage()).contains("stop 2 (S2)", "stop 1 (S1)", "test-graph-v1",
                "connection not found");
        assertThat(run.getResults()).isEmpty();
        assertThat(run.getMatrixJson()).isNull();
        assertThat(run.getStops()).hasSize(4);
        assertThat(runs.count()).isEqualTo(1);
    }

    @Test
    void unroutableDepotIsExplained() {
        when(matrices.buildMatrix(anyList())).thenThrow(new RoadLegUnavailableException(
                RoadLegUnavailableException.Reason.POINT_NOT_ROUTABLE, 0, -1, "too far"));
        EvaluationRun run = service.get(service.evaluate(scenario("10.00"), "planner").getId());
        assertThat(run.getFailureCode()).isEqualTo("POINT_NOT_ROUTABLE");
        assertThat(run.getFailureMessage()).contains("the depot (MN01G)");
    }

    @Test
    void routingUnavailableRetainsNothing() {
        when(matrices.buildMatrix(anyList()))
                .thenThrow(new RoutingUnavailableException("Road snapshot checksum mismatch"));
        assertThatThrownBy(() -> service.evaluate(scenario("10.00"), "planner"))
                .isInstanceOf(RoutingUnavailableException.class);
        assertThat(runs.count()).isZero();
    }

    @Test
    void overCapacityScenarioIsRetainedAsInfeasible() {
        when(matrices.buildMatrix(anyList())).thenReturn(matrix());
        EvaluationRun run = service.get(service.evaluate(scenario("3.00"), "planner").getId());
        assertThat(run.getStatus()).isEqualTo(EvaluationStatus.COMPLETED);
        assertThat(run.getResults()).hasSize(3);
        for (MethodResult r : run.getResults()) {
            assertThat(r.isFeasible()).isFalse();
            assertThat(r.isCapacityOk()).isFalse();
            assertThat(r.visitOrderList()).isEmpty();
            assertThat(r.getLegsGeoJson()).isNull();
        }
    }

    @Test
    void getUnknownRunIs404() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }
}
