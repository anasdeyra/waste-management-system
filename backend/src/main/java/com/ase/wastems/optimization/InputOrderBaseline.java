package com.ase.wastems.optimization;

import java.util.ArrayList;
import java.util.List;

/** Baseline method: visits the stops exactly in the input (selection) order of the draft. */
public final class InputOrderBaseline {

    public static final String CONFIGURATION = "input/selection order of the draft; no reordering";

    public List<Integer> order(PlanningScenario s) {
        List<Integer> order = new ArrayList<>();
        for (int i = 1; i <= s.stopCount(); i++) {
            order.add(i);
        }
        return List.copyOf(order);
    }
}
