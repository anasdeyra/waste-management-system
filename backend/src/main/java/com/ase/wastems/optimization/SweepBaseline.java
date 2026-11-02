package com.ase.wastems.optimization;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sweep baseline after Gillett and Miller (1974), D-052. The stops are sorted by their polar angle
 * around the depot on a local equirectangular projection; every start position, counter-clockwise
 * and clockwise, is evaluated on the exact directed road distance of the closed tour and the
 * cheapest of the 2n candidates is kept. Used here as a single-route sequencing baseline.
 */
public final class SweepBaseline {

    public static final String CONFIGURATION = "Sweep (Gillett and Miller, 1974): stops sorted by polar angle "
            + "around the depot on a local equirectangular projection (ties by planar distance, then node "
            + "index); all 2n start rays, counter-clockwise and clockwise, evaluated on the directed road "
            + "distance and the cheapest kept (ties: counter-clockwise first, then the lowest start "
            + "position); single trip, no depot reload";

    /** The chosen order, the first stop node of it and the sweep direction. */
    public record Result(List<Integer> order, int startNode, boolean clockwise) {}

    /** Polar angle of {@code node} around the depot in [0, 2 pi). */
    static double polarAngle(PlanningScenario s, int node) {
        double x = projectedX(s, node);
        double y = projectedY(s, node);
        double theta = Math.atan2(y, x);
        if (theta < 0) {
            theta += 2 * Math.PI;
        }
        if (theta >= 2 * Math.PI) {
            theta = 0;
        }
        return theta;
    }

    /** Planar distance of {@code node} to the depot on the same projection. */
    static double planarRadius(PlanningScenario s, int node) {
        return Math.hypot(projectedX(s, node), projectedY(s, node));
    }

    private static double projectedX(PlanningScenario s, int node) {
        return (s.longitude(node) - s.longitude(0)) * Math.cos(Math.toRadians(s.latitude(0)));
    }

    private static double projectedY(PlanningScenario s, int node) {
        return s.latitude(node) - s.latitude(0);
    }

    public Result order(PlanningScenario s) {
        if (!s.hasCoordinates()) {
            throw new InfeasibleScenarioException(
                    "The sweep baseline needs the WGS84 coordinates of the depot and of every stop.");
        }
        int n = s.stopCount();
        double[] theta = new double[n + 1];
        double[] radius = new double[n + 1];
        List<Integer> sorted = new ArrayList<>(n);
        for (int node = 1; node <= n; node++) {
            theta[node] = polarAngle(s, node);
            radius[node] = planarRadius(s, node);
            sorted.add(node);
        }
        sorted.sort(Comparator.<Integer>comparingDouble(v -> theta[v])
                .thenComparingDouble(v -> radius[v])
                .thenComparingInt(v -> v));

        DirectedCostMatrix m = s.matrix();
        List<Integer> best = null;
        double bestCost = Double.POSITIVE_INFINITY;
        int bestStart = 0;
        boolean bestClockwise = false;
        for (int dir = 0; dir < 2; dir++) {
            boolean clockwise = dir == 1;
            for (int k = 0; k < n; k++) {
                List<Integer> candidate = new ArrayList<>(n);
                for (int j = 0; j < n; j++) {
                    int idx = clockwise ? Math.floorMod(k - j, n) : (k + j) % n;
                    candidate.add(sorted.get(idx));
                }
                double cost = TourCost.distance(m, candidate);
                if (cost < bestCost) {
                    bestCost = cost;
                    best = candidate;
                    bestStart = candidate.get(0);
                    bestClockwise = clockwise;
                }
            }
        }
        return new Result(List.copyOf(best), bestStart, bestClockwise);
    }
}
