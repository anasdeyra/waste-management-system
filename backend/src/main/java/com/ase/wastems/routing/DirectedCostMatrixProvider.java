package com.ase.wastems.routing;

import java.util.List;

/** Builds the directed matrix of a scenario; the sequence diagram's DirectedCostMatrixProvider. */
public interface DirectedCostMatrixProvider {

    /** nodes.get(0) is the depot, nodes 1..n the stops in input order. */
    RoadMatrix buildMatrix(List<WgsPoint> nodes);

    String roadGraphVersion();

    String routingEngine();
}
