package com.ase.wastems.optimization;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Nearest-neighbour construction followed by a 2-opt local search (D-008).
 *
 * <p>The 2-opt step repeatedly reverses a segment of the tour as long as doing so shortens the
 * tour. Reversing a segment does not change the visited set, so the capacity constraint stays
 * satisfied and the coverage is preserved.
 */
@Component
public class TwoOptTourPlanner extends AbstractTourPlanner {

    private final NearestNeighbourTourPlanner nearestNeighbourPlanner;

    public TwoOptTourPlanner(NearestNeighbourTourPlanner nearestNeighbourPlanner) {
        this.nearestNeighbourPlanner = nearestNeighbourPlanner;
    }

    @Override
    public PlanningMethod method() {
        return PlanningMethod.OPTIMIZED;
    }

    @Override
    public TourPlan plan(PlanningScenario scenario) {
        TourPlan initial = nearestNeighbourPlanner.plan(scenario);
        List<ScenarioPoint> order = toPoints(scenario, initial.visitedPointIds());
        List<ScenarioPoint> improved = twoOpt(scenario, order);
        return buildPlan(scenario, improved, method());
    }

    /**
     * Applies 2-opt until no single reversal shortens the tour.
     *
     * @param scenario the planned scenario
     * @param order the initial visit order
     * @return the improved visit order
     */
    private List<ScenarioPoint> twoOpt(PlanningScenario scenario, List<ScenarioPoint> order) {
        List<ScenarioPoint> best = new ArrayList<>(order);
        double bestDistance = tourDistance(scenario, best);
        boolean improved = true;

        while (improved) {
            improved = false;
            for (int i = 0; i < best.size() - 1; i++) {
                for (int k = i + 1; k < best.size(); k++) {
                    List<ScenarioPoint> candidate = new ArrayList<>(best);
                    reverse(candidate, i, k);
                    double candidateDistance = tourDistance(scenario, candidate);
                    if (candidateDistance + 1e-9 < bestDistance) {
                        best = candidate;
                        bestDistance = candidateDistance;
                        improved = true;
                    }
                }
            }
        }

        return best;
    }

    /** Reverses the segment {@code [from, to]} of the visit order in place. */
    private void reverse(List<ScenarioPoint> order, int from, int to) {
        for (int low = from, high = to; low < high; low++, high--) {
            ScenarioPoint swap = order.get(low);
            order.set(low, order.get(high));
            order.set(high, swap);
        }
    }

    /** Maps the identifiers of a plan back to the scenario points. */
    private List<ScenarioPoint> toPoints(PlanningScenario scenario, List<Long> ids) {
        List<ScenarioPoint> points = new ArrayList<>(ids.size());
        for (Long id : ids) {
            scenario.points().stream()
                    .filter(point -> point.id().equals(id))
                    .findFirst()
                    .ifPresent(points::add);
        }
        return points;
    }
}
