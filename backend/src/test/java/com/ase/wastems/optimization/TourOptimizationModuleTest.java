package com.ase.wastems.optimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class TourOptimizationModuleTest {

    static ArrayCostMatrix m(double[][] d) {
        return new ArrayCostMatrix(d, d);
    }

    static final double[][] C = TwoOptImprovementTest.C;

    @Test
    void threeMethodsOnMatrixC() {
        PlanningScenario s = new PlanningScenario(m(C), new double[] {1, 1, 1, 1}, 10, 600);
        List<MethodOutcome> out = new TourOptimizationModule().planAll(s);
        assertThat(out).extracting(MethodOutcome::method).containsExactly(PlanningMethod.INPUT_ORDER,
                PlanningMethod.NEAREST_NEIGHBOR, PlanningMethod.NN_PLUS_2_OPT);
        assertThat(out.get(0).visitOrder()).containsExactly(1, 2, 3, 4);
        assertThat(out.get(1).visitOrder()).containsExactly(4, 1, 3, 2);
        assertThat(out.get(2).visitOrder()).containsExactly(4, 2, 3, 1);
        double[] dist = {21, 14, 10};
        double[] dur = {2421, 2414, 2410};
        for (int i = 0; i < 3; i++) {
            MethodOutcome o = out.get(i);
            assertThat(o.distanceM()).isCloseTo(dist[i], within(1e-9));
            assertThat(o.travelTimeS()).isCloseTo(dist[i], within(1e-9));
            assertThat(o.durationS()).isCloseTo(dur[i], within(1e-9));
            assertThat(o.feasible()).isTrue();
            assertThat(o.coverageOk()).isTrue();
            assertThat(o.capacityOk()).isTrue();
            assertThat(o.infeasibilityReason()).isNull();
            assertThat(o.executionNanos()).isGreaterThanOrEqualTo(0);
        }
        assertThat(out.get(2).algorithmConfiguration()).contains("no symmetric edge delta")
                .contains("accepted moves 1");
    }

    @Test
    void overCapacityIsInfeasibleForEveryMethod() {
        PlanningScenario s = new PlanningScenario(m(C), new double[] {4, 4, 4, 4}, 10, 600);
        List<MethodOutcome> out = new TourOptimizationModule().planAll(s);
        for (MethodOutcome o : out) {
            assertThat(o.feasible()).isFalse();
            assertThat(o.capacityOk()).isFalse();
            assertThat(o.visitOrder()).isEmpty();
            assertThat(o.distanceM()).isNull();
        }
        assertThat(out.get(0).infeasibilityReason()).contains("exceeds vehicle capacity");
        assertThat(out.get(1).infeasibilityReason()).contains("no remaining stop fits");
        assertThat(out.get(2).infeasibilityReason()).contains("no remaining stop fits");
    }

    @Test
    void singleStopScenario() {
        PlanningScenario s = new PlanningScenario(m(new double[][] {{0, 7}, {3, 0}}), new double[] {5}, 5, 600);
        for (MethodOutcome o : new TourOptimizationModule().planAll(s)) {
            assertThat(o.visitOrder()).containsExactly(1);
            assertThat(o.distanceM()).isCloseTo(10, within(1e-9));
            assertThat(o.durationS()).isCloseTo(610, within(1e-9));
        }
    }

    private static PlanningScenario random30() {
        int n = 30;
        Random rnd = new Random(42);
        double[][] d = new double[n + 1][n + 1];
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                d[i][j] = i == j ? 0 : 1 + rnd.nextInt(1000);
            }
        }
        double[] demand = new double[n];
        java.util.Arrays.fill(demand, 1);
        return new PlanningScenario(m(d), demand, 100, 600);
    }

    @Test
    void deterministicAcrossRuns() {
        List<MethodOutcome> a = new TourOptimizationModule().planAll(random30());
        List<MethodOutcome> b = new TourOptimizationModule().planAll(random30());
        for (int i = 0; i < 3; i++) {
            assertThat(a.get(i).visitOrder()).isEqualTo(b.get(i).visitOrder());
            assertThat(a.get(i).distanceM()).isEqualTo(b.get(i).distanceM());
        }
    }

    @Test
    void reportedDistanceMatchesRecomputation() {
        PlanningScenario s = random30();
        List<MethodOutcome> out = new TourOptimizationModule().planAll(s);
        for (MethodOutcome o : out) {
            assertThat(o.feasible()).isTrue();
            assertThat(o.distanceM()).isEqualTo(TourCost.distance(s.matrix(), o.visitOrder()));
        }
        assertThat(out.get(2).distanceM()).isLessThanOrEqualTo(out.get(1).distanceM());
    }

    @Test
    void invalidInputsAreRejected() {
        assertThatThrownBy(() -> new ArrayCostMatrix(new double[][] {{0, 1}, {1}}, new double[][] {{0, 1}, {1}}))
                .isInstanceOf(IllegalArgumentException.class);
        double[][] neg = {{0, -1}, {1, 0}};
        double[][] ok = {{0, 1}, {1, 0}};
        assertThatThrownBy(() -> new ArrayCostMatrix(neg, ok)).isInstanceOf(IllegalArgumentException.class);
        double[][] nan = {{0, Double.NaN}, {1, 0}};
        assertThatThrownBy(() -> new ArrayCostMatrix(ok, nan)).isInstanceOf(IllegalArgumentException.class);
        ArrayCostMatrix mat = m(ok);
        assertThatThrownBy(() -> new PlanningScenario(mat, new double[] {1, 1}, 10, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlanningScenario(mat, new double[] {1}, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlanningScenario(mat, new double[] {1}, 10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
