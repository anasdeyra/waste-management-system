package com.ase.wastems.optimization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the three planning methods behave as documented in D-008 and that the
 * optimized plan never violates the capacity constraint.
 */
class TourPlannerTest {

    private static final double CAPACITY_KG = 1000;
    private static final double SPEED_KMH = 40;
    private static final double SERVICE_MINUTES = 10;

    /** Four points arranged so that the input order is clearly worse than the optimal one. */
    private PlanningScenario scenario() {
        List<ScenarioPoint> points =
                List.of(
                        new ScenarioPoint(1L, 0.0, 10.0, 100),
                        new ScenarioPoint(2L, 10.0, 10.0, 100),
                        new ScenarioPoint(3L, 0.0, 0.0, 100),
                        new ScenarioPoint(4L, 10.0, 0.0, 100));
        return new PlanningScenario("unit-test", 0.0, 0.0, points, CAPACITY_KG, SPEED_KMH, SERVICE_MINUTES);
    }

    @Test
    @DisplayName("The baseline visits the points in the order of the scenario")
    void baselineKeepsInputOrder() {
        TourPlan plan = new BaselineTourPlanner().plan(scenario());

        assertThat(plan.method()).isEqualTo(PlanningMethod.BASELINE);
        assertThat(plan.visitedPointIds()).containsExactly(1L, 2L, 3L, 4L);
        assertThat(plan.constraintViolations()).isZero();
    }

    @Test
    @DisplayName("The nearest neighbour starts with the closest point")
    void nearestNeighbourStartsClose() {
        TourPlan plan = new NearestNeighbourTourPlanner().plan(scenario());

        assertThat(plan.method()).isEqualTo(PlanningMethod.NEAREST_NEIGHBOR);
        assertThat(plan.visitedPointIds().getFirst()).isEqualTo(3L);
        assertThat(plan.constraintViolations()).isZero();
    }

    @Test
    @DisplayName("The optimized plan is never longer than the baseline plan")
    void optimizedIsNotWorseThanBaseline() {
        PlanningScenario scenario = scenario();

        TourPlan baseline = new BaselineTourPlanner().plan(scenario);
        TourPlan optimized = new TwoOptTourPlanner(new NearestNeighbourTourPlanner()).plan(scenario);

        assertThat(optimized.totalDistanceKm()).isLessThanOrEqualTo(baseline.totalDistanceKm());
        assertThat(optimized.constraintViolations()).isZero();
        assertThat(optimized.coveredPoints()).isEqualTo(scenario.points().size());
    }

    @Test
    @DisplayName("A point that does not fit into the vehicle is reported as a constraint violation")
    void oversizedDemandIsReported() {
        List<ScenarioPoint> points =
                List.of(
                        new ScenarioPoint(1L, 0.0, 0.0, 900),
                        new ScenarioPoint(2L, 5.0, 5.0, 900));
        PlanningScenario scenario =
                new PlanningScenario("too-heavy", 0.0, 0.0, points, CAPACITY_KG, SPEED_KMH, SERVICE_MINUTES);

        TourPlan plan = new NearestNeighbourTourPlanner().plan(scenario);

        assertThat(plan.coveredPoints()).isEqualTo(1);
        assertThat(plan.constraintViolations()).isEqualTo(1);
    }

    @Test
    @DisplayName("The duration model follows the documented formula")
    void durationMatchesFormula() {
        PlanningScenario scenario = scenario();
        TourPlan plan = new BaselineTourPlanner().plan(scenario);

        double expectedTravelMinutes = (plan.totalDistanceKm() / SPEED_KMH) * 60.0;
        double expectedServiceMinutes = 4 * SERVICE_MINUTES;

        assertThat(plan.estimatedDurationMinutes())
                .isCloseTo(expectedTravelMinutes + expectedServiceMinutes, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    @DisplayName("The planners are deterministic")
    void plannersAreDeterministic() {
        NearestNeighbourTourPlanner planner = new NearestNeighbourTourPlanner();
        PlanningScenario scenario = scenario();

        List<Long> first = new ArrayList<>(planner.plan(scenario).visitedPointIds());
        List<Long> second = new ArrayList<>(planner.plan(scenario).visitedPointIds());

        assertThat(first).isEqualTo(second);
    }
}
