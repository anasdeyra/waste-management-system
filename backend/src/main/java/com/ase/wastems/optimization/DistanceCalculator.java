package com.ase.wastems.optimization;

/** Euclidean distance between two planning points, in kilometres. */
public final class DistanceCalculator {

    private DistanceCalculator() {
        // Utility class.
    }

    /**
     * Returns the straight-line distance between {@code (x1, y1)} and {@code (x2, y2)}.
     *
     * <p>The metric is deliberately simple: D-009 fixes Euclidean distance on synthetic
     * coordinates so that the evaluation does not depend on an external routing service.
     */
    public static double euclidean(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
