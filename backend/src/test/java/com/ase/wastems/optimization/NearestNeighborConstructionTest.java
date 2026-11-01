package com.ase.wastems.optimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NearestNeighborConstructionTest {

    static ArrayCostMatrix m(double[][] d) {
        return new ArrayCostMatrix(d, d);
    }

    static final double[][] C = TwoOptImprovementTest.C;
    static final double[][] T = {{0, 4, 4, 9}, {9, 0, 9, 3}, {9, 3, 0, 9}, {9, 9, 9, 0}};

    private final NearestNeighborConstruction nn = new NearestNeighborConstruction();

    @Test
    void picksNearestByDirectedDistance() {
        assertThat(nn.construct(new PlanningScenario(m(C), new double[] {1, 1, 1, 1}, 10, 0)))
                .containsExactly(4, 1, 3, 2);
    }

    @Test
    void tiesGoToLowestIndex() {
        assertThat(nn.construct(new PlanningScenario(m(T), new double[] {1, 1, 1}, 10, 0)))
                .containsExactly(1, 3, 2);
    }

    @Test
    void throwsWhenRemainingStopsDoNotFit() {
        // In a single trip the capacity filter can only bind when total demand exceeds capacity.
        PlanningScenario s = new PlanningScenario(m(C), new double[] {6, 1, 1, 3}, 10, 0);
        assertThatThrownBy(() -> nn.construct(s)).isInstanceOf(InfeasibleScenarioException.class)
                .hasMessageContaining("after 3 of 4 stops").hasMessageContaining("remaining demand 1.00 kg");
    }

    @Test
    void exactCapacityIsFeasible() {
        assertThat(nn.construct(new PlanningScenario(m(C), new double[] {2.5, 2.5, 2.5, 2.5}, 10, 0)))
                .containsExactly(4, 1, 3, 2);
    }
}
