package com.ase.wastems.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ase.wastems.service.DemandProfile;
import com.ase.wastems.service.ExperimentSpec;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExperimentCommandRunnerTest {

    @Test
    void allNullPropertiesGiveTheCanonicalSpec() {
        ExperimentSpec s = ExperimentCommandRunner.specFrom(
                new ExperimentProperties(new BigDecimal("10000"), null, null, null, null, null, "out"));
        assertThat(s.isCanonical()).isTrue();
        assertThat(s.runCount()).isEqualTo(60);
    }

    @Test
    void propertiesAreNormalized() {
        ExperimentSpec s = ExperimentCommandRunner.specFrom(new ExperimentProperties(new BigDecimal("10000"),
                List.of(DemandProfile.HOTSPOT), List.of(2L), List.of(5, 3), 2, "x", "out"));
        assertThat(s.sizes()).containsExactly(3, 5);
        assertThat(s.runCount()).isEqualTo(4);
        assertThat(s.label()).isEqualTo("x");
        assertThat(s.isCanonical()).isFalse();
    }
}
