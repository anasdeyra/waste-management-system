package com.ase.wastems.optimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Capacity-aware nearest neighbor construction on directed road distance. From the current
 * node it always moves to the nearest unvisited stop whose demand still fits the remaining
 * capacity; ties go to the lowest input index. Single trip from and back to the depot.
 */
public final class NearestNeighborConstruction {

    public static final String CONFIGURATION = "nearest neighbor from the depot on directed road distance; "
            + "ties to the lowest input index; a stop is a candidate only if its demand fits the "
            + "remaining capacity (single trip, no depot reload)";

    public List<Integer> construct(PlanningScenario s) {
        DirectedCostMatrix m = s.matrix();
        int n = s.stopCount();
        int current = 0;
        double load = 0.0;
        boolean[] visited = new boolean[n + 1];
        List<Integer> order = new ArrayList<>();
        for (int step = 0; step < n; step++) {
            int best = -1;
            for (int j = 1; j <= n; j++) {
                if (visited[j]) {
                    continue;
                }
                if (load + s.demandKg(j) > s.capacityKg() + 1e-9) {
                    continue;
                }
                if (best == -1 || m.distance(current, j) < m.distance(current, best)) {
                    best = j;
                }
            }
            if (best == -1) {
                double remaining = 0;
                for (int j = 1; j <= n; j++) {
                    if (!visited[j]) {
                        remaining += s.demandKg(j);
                    }
                }
                throw new InfeasibleScenarioException(String.format(Locale.ROOT,
                        "Nearest neighbor stopped after %d of %d stops: no remaining stop fits the "
                                + "remaining capacity of %.2f kg (remaining demand %.2f kg).",
                        order.size(), n, s.capacityKg() - load, remaining));
            }
            order.add(best);
            visited[best] = true;
            load += s.demandKg(best);
            current = best;
        }
        return List.copyOf(order);
    }
}
