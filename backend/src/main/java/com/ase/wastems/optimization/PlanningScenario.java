package com.ase.wastems.optimization;

/**
 * Immutable input shared by all three methods: the directed matrix (node 0 = depot, nodes
 * 1..n = stops in input order), one demand per stop, the single vehicle capacity and the
 * service time per stop.
 */
public final class PlanningScenario {

    private final DirectedCostMatrix matrix;
    private final double[] demandKg;
    private final double capacityKg;
    private final double serviceTimeSeconds;

    public PlanningScenario(DirectedCostMatrix matrix, double[] demandKg, double capacityKg,
            double serviceTimeSeconds) {
        if (matrix == null) {
            throw new IllegalArgumentException("The cost matrix is required.");
        }
        if (matrix.size() < 2) {
            throw new IllegalArgumentException("A scenario needs the depot and at least one stop.");
        }
        if (demandKg == null || demandKg.length != matrix.size() - 1) {
            throw new IllegalArgumentException("There must be exactly one demand per stop.");
        }
        for (double d : demandKg) {
            if (!Double.isFinite(d) || d < 0) {
                throw new IllegalArgumentException("Invalid stop demand: " + d);
            }
        }
        if (!Double.isFinite(capacityKg) || capacityKg <= 0) {
            throw new IllegalArgumentException("The vehicle capacity must be positive.");
        }
        if (!Double.isFinite(serviceTimeSeconds) || serviceTimeSeconds < 0) {
            throw new IllegalArgumentException("The service time must not be negative.");
        }
        this.matrix = matrix;
        this.demandKg = demandKg.clone();
        this.capacityKg = capacityKg;
        this.serviceTimeSeconds = serviceTimeSeconds;
    }

    public DirectedCostMatrix matrix() {
        return matrix;
    }

    public int stopCount() {
        return demandKg.length;
    }

    /** Demand of stop node {@code stopNode} (1..n). */
    public double demandKg(int stopNode) {
        return demandKg[stopNode - 1];
    }

    public double totalDemandKg() {
        double sum = 0;
        for (double d : demandKg) {
            sum += d;
        }
        return sum;
    }

    public double capacityKg() {
        return capacityKg;
    }

    public double serviceTimeSeconds() {
        return serviceTimeSeconds;
    }
}
