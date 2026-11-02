package com.ase.wastems.service;

import com.ase.wastems.optimization.PlanningMethod;
import com.ase.wastems.repository.RunMethodRow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import com.ase.wastems.domain.EvaluationStatus;

/**
 * Aggregates of a batch: mean of the per-run percentage changes against the input order, over
 * comparable runs only (completed, both results feasible, input-order distance above zero). The
 * changes against the sweep baseline (NN and NN + 2-opt only, D-052) use the same rule with sweep
 * as the reference; a run without a sweep result (retained before S11) is not comparable.
 */
public final class EvaluationAggregates {

    private static final List<String> PROFILE_ORDER = List.of("UNIFORM_LOW", "UNIFORM_HIGH", "HOTSPOT");

    private EvaluationAggregates() {}

    private record Run(UUID id, EvaluationStatus status, String profile, Integer pointCount,
            Map<PlanningMethod, RunMethodRow> methods) {}

    public static List<AggregateRow> compute(List<RunMethodRow> rows) {
        Map<UUID, Run> byRun = new LinkedHashMap<>();
        for (RunMethodRow r : rows) {
            Run run = byRun.computeIfAbsent(r.runId(),
                    id -> new Run(id, r.status(), r.demandProfile(), r.pointCount(), new LinkedHashMap<>()));
            if (r.method() != null) {
                run.methods().put(r.method(), r);
            }
        }
        List<Run> all = new ArrayList<>(byRun.values());
        List<AggregateRow> out = new ArrayList<>();
        if (all.isEmpty()) {
            return out;
        }
        addGroup(out, "ALL", "all", all);
        all.stream().map(Run::pointCount).filter(java.util.Objects::nonNull).distinct().sorted().forEach(size ->
                addGroup(out, "SIZE", String.valueOf(size), all.stream().filter(r -> size.equals(r.pointCount())).toList()));
        all.stream().map(Run::profile).filter(java.util.Objects::nonNull).distinct()
                .sorted(Comparator.<String>comparingInt(p -> {
                    int i = PROFILE_ORDER.indexOf(p);
                    return i < 0 ? PROFILE_ORDER.size() : i;
                }).thenComparing(Function.identity()))
                .forEach(p -> addGroup(out, "PROFILE", p, all.stream().filter(r -> p.equals(r.profile())).toList()));
        return out;
    }

    private static void addGroup(List<AggregateRow> out, String group, String value, List<Run> runs) {
        for (PlanningMethod method : PlanningMethod.values()) {
            int comparable = 0;
            double sumDist = 0;
            double sumDur = 0;
            List<Double> distChange = new ArrayList<>();
            List<Double> durChange = new ArrayList<>();
            List<Double> distVsSweep = new ArrayList<>();
            List<Double> durVsSweep = new ArrayList<>();
            double sumExec = 0;
            int execCount = 0;
            for (Run run : runs) {
                RunMethodRow m = run.methods().get(method);
                RunMethodRow io = run.methods().get(PlanningMethod.INPUT_ORDER);
                if (m != null && m.executionNanos() != null) {
                    sumExec += m.executionNanos() / 1e6;
                    execCount++;
                }
                boolean completed = run.status() == EvaluationStatus.COMPLETED;
                boolean mFeasible = m != null && Boolean.TRUE.equals(m.feasible());
                boolean ioFeasible = io != null && Boolean.TRUE.equals(io.feasible());
                RunMethodRow sw = run.methods().get(PlanningMethod.SWEEP);
                if (EvaluationComparison.comparedWithSweep(method) && completed && mFeasible && sw != null
                        && Boolean.TRUE.equals(sw.feasible()) && sw.distanceM() != null && sw.distanceM() > 0) {
                    distVsSweep.add(EvaluationComparison.changePct(true, true, m.distanceM(), true, sw.distanceM()));
                    durVsSweep.add(EvaluationComparison.changePct(true, true, m.durationS(), true, sw.durationS()));
                }
                if (!completed || !mFeasible || !ioFeasible || io.distanceM() == null || io.distanceM() <= 0) {
                    continue;
                }
                comparable++;
                sumDist += m.distanceM();
                sumDur += m.durationS();
                distChange.add(EvaluationComparison.changePct(true, true, m.distanceM(), true, io.distanceM()));
                durChange.add(EvaluationComparison.changePct(true, true, m.durationS(), true, io.durationS()));
            }
            out.add(new AggregateRow(group, value, method, runs.size(), comparable, runs.size() - comparable,
                    comparable == 0 ? null : sumDist / comparable, comparable == 0 ? null : sumDur / comparable,
                    mean(distChange), min(distChange), max(distChange), mean(durChange), min(durChange),
                    max(durChange), execCount == 0 ? null : sumExec / execCount, distVsSweep.size(),
                    mean(distVsSweep), min(distVsSweep), max(distVsSweep), mean(durVsSweep)));
        }
    }

    private static Double mean(List<Double> v) {
        return v.isEmpty() ? null : v.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private static Double min(List<Double> v) {
        return v.isEmpty() ? null : v.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
    }

    private static Double max(List<Double> v) {
        return v.isEmpty() ? null : v.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
    }
}
