package com.ase.wastems.routing;

import com.ase.wastems.optimization.DirectedCostMatrix;

/**
 * Directed distance/time matrix built on one road graph version, with the road-following leg of
 * every ordered node pair. Implements the framework-free matrix interface of the optimization core.
 */
public final class RoadMatrix implements DirectedCostMatrix {

    private final String roadGraphVersion;
    private final double[][] distanceM;
    private final double[][] travelTimeS;
    private final RoadLeg[][] legs;

    public RoadMatrix(String roadGraphVersion, double[][] distanceM, double[][] travelTimeS, RoadLeg[][] legs) {
        int n = distanceM.length;
        if (travelTimeS.length != n || legs.length != n) {
            throw new IllegalArgumentException("Matrix sizes differ.");
        }
        this.roadGraphVersion = roadGraphVersion;
        this.distanceM = new double[n][];
        this.travelTimeS = new double[n][];
        this.legs = new RoadLeg[n][];
        for (int i = 0; i < n; i++) {
            if (distanceM[i].length != n || travelTimeS[i].length != n || legs[i].length != n) {
                throw new IllegalArgumentException("Matrices must be square.");
            }
            this.distanceM[i] = distanceM[i].clone();
            this.travelTimeS[i] = travelTimeS[i].clone();
            this.legs[i] = legs[i].clone();
        }
    }

    @Override
    public int size() {
        return distanceM.length;
    }

    @Override
    public double distance(int from, int to) {
        return distanceM[from][to];
    }

    @Override
    public double travelTime(int from, int to) {
        return travelTimeS[from][to];
    }

    public RoadLeg leg(int from, int to) {
        return legs[from][to];
    }

    public String roadGraphVersion() {
        return roadGraphVersion;
    }

    public double[][] distanceMatrix() {
        return deepCopy(distanceM);
    }

    public double[][] travelTimeMatrix() {
        return deepCopy(travelTimeS);
    }

    private static double[][] deepCopy(double[][] src) {
        double[][] copy = new double[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i].clone();
        }
        return copy;
    }
}
