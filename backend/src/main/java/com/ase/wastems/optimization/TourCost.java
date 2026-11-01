package com.ase.wastems.optimization;

import java.util.List;

/**
 * Exact directed cost of a tour that starts at the depot (node 0), visits the given stop nodes
 * in order and returns to the depot.
 */
public final class TourCost {

    private TourCost() {}

    /** d(0,o1) + sum d(ok,ok+1) + d(on,0); 0 for an empty order. */
    public static double distance(DirectedCostMatrix m, List<Integer> order) {
        if (order.isEmpty()) {
            return 0;
        }
        double sum = m.distance(0, order.get(0));
        for (int k = 0; k + 1 < order.size(); k++) {
            sum += m.distance(order.get(k), order.get(k + 1));
        }
        return sum + m.distance(order.get(order.size() - 1), 0);
    }

    /** Same as {@link #distance} on the directed base travel times. */
    public static double travelTime(DirectedCostMatrix m, List<Integer> order) {
        if (order.isEmpty()) {
            return 0;
        }
        double sum = m.travelTime(0, order.get(0));
        for (int k = 0; k + 1 < order.size(); k++) {
            sum += m.travelTime(order.get(k), order.get(k + 1));
        }
        return sum + m.travelTime(order.get(order.size() - 1), 0);
    }
}
