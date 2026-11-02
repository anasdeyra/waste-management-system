package com.ase.wastems.optimization;

/**
 * Immutable input shared by all four methods: the directed matrix (node 0 = depot, nodes
 * 1..n = stops in input order), one demand per stop, the single vehicle capacity and the
 * service time per stop.
 */
public final class PlanningScenario {

    private final DirectedCostMatrix matrix;
    private final double[] demandKg;
    private final double capacityKg;
    private final double serviceTimeSeconds;
    private final double[] longitudes;
    private final double[] latitudes;

    /** A scenario without coordinates: the sweep baseline is then infeasible (D-052). */
    public PlanningScenario(DirectedCostMatrix matrix, double[] demandKg, double capacityKg,
            double serviceTimeSeconds) {
        this(matrix, demandKg, capacityKg, serviceTimeSeconds, null, null);
    }

    /** Optional WGS84 coordinates of every node (index 0 = depot), both arrays or neither. */
    public PlanningScenario(DirectedCostMatrix matrix, double[] demandKg, double capacityKg,
            double serviceTimeSeconds, double[] longitudes, double[] latitudes) {
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
        if ((longitudes == null) != (latitudes == null)) {
            throw new IllegalArgumentException("Longitudes and latitudes must both be given or both be absent.");
        }
        if (longitudes != null) {
            if (longitudes.length != matrix.size() || latitudes.length != matrix.size()) {
                throw new IllegalArgumentException("There must be exactly one coordinate pair per node.");
            }
            for (int i = 0; i < longitudes.length; i++) {
                if (!Double.isFinite(longitudes[i]) || longitudes[i] < -180 || longitudes[i] > 180) {
                    throw new IllegalArgumentException("Invalid longitude: " + longitudes[i]);
                }
                if (!Double.isFinite(latitudes[i]) || latitudes[i] < -90 || latitudes[i] > 90) {
                    throw new IllegalArgumentException("Invalid latitude: " + latitudes[i]);
                }
            }
        }
        this.longitudes = longitudes == null ? null : longitudes.clone();
        this.latitudes = latitudes == null ? null : latitudes.clone();
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

    public boolean hasCoordinates() {
        return longitudes != null;
    }

    /** Longitude of node {@code node} (0 = depot); only valid when {@link #hasCoordinates()}. */
    public double longitude(int node) {
        return longitudes[node];
    }

    /** Latitude of node {@code node} (0 = depot); only valid when {@link #hasCoordinates()}. */
    public double latitude(int node) {
        return latitudes[node];
    }
}
