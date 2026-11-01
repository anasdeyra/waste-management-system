package com.ase.wastems.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EvaluationComparisonTest {

    @Test
    void reductionIsNegative() {
        assertThat(EvaluationComparison.changePct(true, true, 750.0, true, 1000.0)).isEqualTo(-25.0);
    }

    @Test
    void notComparableGivesNull() {
        assertThat(EvaluationComparison.changePct(true, true, 750.0, true, 0.0)).isNull();
        assertThat(EvaluationComparison.changePct(true, true, 750.0, true, null)).isNull();
        assertThat(EvaluationComparison.changePct(true, false, 750.0, true, 1000.0)).isNull();
        assertThat(EvaluationComparison.changePct(true, true, 750.0, false, 1000.0)).isNull();
        assertThat(EvaluationComparison.changePct(false, true, 750.0, true, 1000.0)).isNull();
        assertThat(EvaluationComparison.changePct(true, true, null, true, 1000.0)).isNull();
    }
}
