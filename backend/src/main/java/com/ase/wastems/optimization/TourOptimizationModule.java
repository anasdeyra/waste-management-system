package com.ase.wastems.optimization;

import java.util.List;
import java.util.Locale;

/**
 * Entry point of the intelligent module (the sequence diagram's TourOptimizationModule). It runs
 * the planning methods on one shared scenario, measures each method's execution time and applies
 * the same capacity and coverage validation to every method so that comparisons are fair.
 */
public final class TourOptimizationModule {

    private final InputOrderBaseline baseline = new InputOrderBaseline();
    private final NearestNeighborConstruction nn = new NearestNeighborConstruction();
    private final TwoOptImprovement twoOpt = new TwoOptImprovement();

    public MethodOutcome plan(PlanningMethod method, PlanningScenario s) {
        DirectedCostMatrix m = s.matrix();
        long t0 = System.nanoTime();
        List<Integer> order = null;
        String reason = null;
        String config;
        switch (method) {
            case INPUT_ORDER -> {
                order = baseline.order(s);
                config = InputOrderBaseline.CONFIGURATION;
            }
            case NEAREST_NEIGHBOR -> {
                config = NearestNeighborConstruction.CONFIGURATION;
                try {
                    order = nn.construct(s);
                } catch (InfeasibleScenarioException e) {
                    reason = e.getMessage();
                }
            }
            case NN_PLUS_2_OPT -> {
                config = NearestNeighborConstruction.CONFIGURATION + "; then " + TwoOptImprovement.CONFIGURATION;
                try {
                    TwoOptImprovement.Result r = twoOpt.improve(m, nn.construct(s));
                    order = r.order();
                    config += "; accepted moves " + r.acceptedMoves();
                } catch (InfeasibleScenarioException e) {
                    reason = e.getMessage();
                }
            }
            default -> throw new IllegalArgumentException("Unknown method " + method);
        }
        long nanos = System.nanoTime() - t0;

        boolean capacityOk = s.totalDemandKg() <= s.capacityKg() + 1e-9;
        boolean coverageOk = order != null && isPermutation(order, s.stopCount());
        if (reason == null && !capacityOk) {
            reason = String.format(Locale.ROOT, "Total demand %.2f kg exceeds vehicle capacity %.2f kg.",
                    s.totalDemandKg(), s.capacityKg());
        }
        if (reason == null && !coverageOk) {
            reason = "The order does not visit every stop exactly once.";
        }
        if (reason != null) {
            return new MethodOutcome(method, false, List.of(), null, null, null, nanos, coverageOk,
                    capacityOk, reason, config);
        }
        double travel = TourCost.travelTime(m, order);
        return new MethodOutcome(method, true, order, TourCost.distance(m, order), travel,
                travel + s.stopCount() * s.serviceTimeSeconds(), nanos, true, true, null, config);
    }

    public List<MethodOutcome> planAll(PlanningScenario s) {
        return List.of(plan(PlanningMethod.INPUT_ORDER, s), plan(PlanningMethod.NEAREST_NEIGHBOR, s),
                plan(PlanningMethod.NN_PLUS_2_OPT, s));
    }

    private static boolean isPermutation(List<Integer> order, int n) {
        if (order.size() != n) {
            return false;
        }
        boolean[] seen = new boolean[n + 1];
        for (int v : order) {
            if (v < 1 || v > n || seen[v]) {
                return false;
            }
            seen[v] = true;
        }
        return true;
    }
}
