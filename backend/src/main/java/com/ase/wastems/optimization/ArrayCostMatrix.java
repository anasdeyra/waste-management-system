package com.ase.wastems.optimization;

/**
 * Array-backed {@link DirectedCostMatrix}. The constructor deep-copies the inputs and rejects
 * non-square, mismatched, empty, NaN, infinite or negative data.
 */
public final class ArrayCostMatrix implements DirectedCostMatrix {

    private final double[][] distanceM;
    private final double[][] travelTimeS;

    public ArrayCostMatrix(double[][] distanceM, double[][] travelTimeS) {
        this.distanceM = copyValidated(distanceM, "distance");
        this.travelTimeS = copyValidated(travelTimeS, "travel time");
        if (this.distanceM.length != this.travelTimeS.length) {
            throw new IllegalArgumentException("Distance and travel time matrices differ in size.");
        }
    }

    private static double[][] copyValidated(double[][] source, String label) {
        if (source == null || source.length < 1) {
            throw new IllegalArgumentException("The " + label + " matrix must have at least one row.");
        }
        double[][] copy = new double[source.length][];
        for (int i = 0; i < source.length; i++) {
            if (source[i] == null || source[i].length != source.length) {
                throw new IllegalArgumentException("The " + label + " matrix must be square.");
            }
            for (double v : source[i]) {
                if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) {
                    throw new IllegalArgumentException(
                            "The " + label + " matrix holds an invalid value: " + v);
                }
            }
            copy[i] = source[i].clone();
        }
        return copy;
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
}
