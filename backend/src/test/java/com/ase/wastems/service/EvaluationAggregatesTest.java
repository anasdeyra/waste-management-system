package com.ase.wastems.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ase.wastems.domain.EvaluationStatus;
import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.repository.RunMethodRow;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvaluationAggregatesTest {

    private static final long NANOS = 1_000_000L;

    private static RunMethodRow row(UUID id, EvaluationStatus st, String key, String profile, int size,
            PlanningMethod m, Double dist, Double dur) {
        boolean feasible = dist != null;
        return new RunMethodRow(id, st, key, profile, size, m, feasible, dist, dur, NANOS, true, feasible);
    }

    private List<AggregateRow> rows() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        List<RunMethodRow> rows = new ArrayList<>();
        EvaluationStatus ok = EvaluationStatus.COMPLETED;
        rows.add(row(a, ok, "A", "UNIFORM_LOW", 10, PlanningMethod.INPUT_ORDER, 1000.0, 2000.0));
        rows.add(row(a, ok, "A", "UNIFORM_LOW", 10, PlanningMethod.NEAREST_NEIGHBOR, 800.0, 1800.0));
        rows.add(row(a, ok, "A", "UNIFORM_LOW", 10, PlanningMethod.NN_PLUS_2_OPT, 700.0, 1700.0));
        rows.add(row(b, ok, "B", "HOTSPOT", 10, PlanningMethod.INPUT_ORDER, 2000.0, 3000.0));
        rows.add(row(b, ok, "B", "HOTSPOT", 10, PlanningMethod.NEAREST_NEIGHBOR, 1500.0, 2500.0));
        rows.add(row(b, ok, "B", "HOTSPOT", 10, PlanningMethod.NN_PLUS_2_OPT, 1000.0, 2400.0));
        rows.add(new RunMethodRow(c, EvaluationStatus.ROUTING_FAILED, "C", "UNIFORM_LOW", 25, null, null, null, null,
                null, null, null));
        for (PlanningMethod m : PlanningMethod.values()) {
            rows.add(row(d, ok, "D", "HOTSPOT", 25, m, null, null));
        }
        return EvaluationAggregates.compute(rows);
    }

    private static AggregateRow find(List<AggregateRow> l, String g, String v, PlanningMethod m) {
        return l.stream().filter(r -> r.group().equals(g) && r.value().equals(v) && r.method() == m).findFirst()
                .orElseThrow();
    }

    @Test
    void groupsAndStatistics() {
        List<AggregateRow> l = rows();
        assertThat(l).hasSize(15);
        assertThat(l.subList(0, 3)).allMatch(r -> r.group().equals("ALL"));
        assertThat(l.subList(3, 9)).extracting(AggregateRow::value).containsExactly("10", "10", "10", "25", "25", "25");
        assertThat(l.subList(9, 15)).extracting(AggregateRow::value)
                .containsExactly("UNIFORM_LOW", "UNIFORM_LOW", "UNIFORM_LOW", "HOTSPOT", "HOTSPOT", "HOTSPOT");
        AggregateRow two = find(l, "ALL", "all", PlanningMethod.NN_PLUS_2_OPT);
        assertThat(two.runs()).isEqualTo(4);
        assertThat(two.comparableRuns()).isEqualTo(2);
        assertThat(two.excludedRuns()).isEqualTo(2);
        assertThat(two.meanDistanceChangePct()).isEqualTo(-40.0);
        assertThat(two.minDistanceChangePct()).isEqualTo(-50.0);
        assertThat(two.maxDistanceChangePct()).isEqualTo(-30.0);
        assertThat(two.meanDurationChangePct()).isEqualTo(-17.5);
        assertThat(two.meanDistanceM()).isEqualTo(850.0);
        AggregateRow io = find(l, "ALL", "all", PlanningMethod.INPUT_ORDER);
        assertThat(io.comparableRuns()).isEqualTo(2);
        assertThat(io.meanDistanceChangePct()).isEqualTo(0.0);
        assertThat(find(l, "SIZE", "10", PlanningMethod.NEAREST_NEIGHBOR).meanDistanceChangePct()).isEqualTo(-22.5);
        for (PlanningMethod m : PlanningMethod.values()) {
            AggregateRow s25 = find(l, "SIZE", "25", m);
            assertThat(s25.comparableRuns()).isZero();
            assertThat(s25.meanDistanceM()).isNull();
            assertThat(s25.meanDistanceChangePct()).isNull();
            assertThat(s25.meanExecutionTimeMs()).isEqualTo(1.0);
        }
        assertThat(two.meanExecutionTimeMs()).isEqualTo(1.0);
    }
}
