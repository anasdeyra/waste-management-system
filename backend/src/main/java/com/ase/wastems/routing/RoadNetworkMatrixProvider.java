package com.ase.wastems.routing;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds the directed matrix with one router request per ordered node pair. Every node is
 * snapped before any leg is routed; values are rounded to 3 decimals so that the optimizer uses
 * exactly the retained numbers. Traffic never enters (D-021).
 */
@Component
public class RoadNetworkMatrixProvider implements DirectedCostMatrixProvider {

    private final RoadRouter router;

    public RoadNetworkMatrixProvider(RoadRouter router) {
        this.router = router;
    }

    @Override
    public RoadMatrix buildMatrix(List<WgsPoint> nodes) {
        if (nodes == null || nodes.size() < 2) {
            throw new IllegalArgumentException("A scenario needs the depot and at least one stop.");
        }
        String version = router.roadGraphVersion();
        int n = nodes.size();
        for (int i = 0; i < n; i++) {
            try {
                router.snapDistanceMeters(nodes.get(i));
            } catch (RoadLegUnavailableException e) {
                throw new RoadLegUnavailableException(RoadLegUnavailableException.Reason.POINT_NOT_ROUTABLE,
                        i, -1, e.getMessage());
            }
        }
        double[][] dist = new double[n][n];
        double[][] time = new double[n][n];
        RoadLeg[][] legs = new RoadLeg[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    legs[i][j] = new RoadLeg(0, 0, List.of(nodes.get(i)));
                    continue;
                }
                RoadLeg leg;
                try {
                    leg = router.route(nodes.get(i), nodes.get(j));
                } catch (RoadLegUnavailableException e) {
                    throw new RoadLegUnavailableException(RoadLegUnavailableException.Reason.NO_ROAD_PATH,
                            i, j, e.getMessage());
                }
                dist[i][j] = round3(leg.distanceM());
                time[i][j] = round3(leg.travelTimeS());
                legs[i][j] = leg;
            }
        }
        return new RoadMatrix(version, dist, time, legs);
    }

    @Override
    public String roadGraphVersion() {
        return router.roadGraphVersion();
    }

    @Override
    public String routingEngine() {
        return router.routingEngine();
    }

    private static double round3(double x) {
        return Math.round(x * 1000.0) / 1000.0;
    }
}
