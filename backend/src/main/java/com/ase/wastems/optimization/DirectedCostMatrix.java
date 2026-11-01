package com.ase.wastems.optimization;

/**
 * Directed road costs between the nodes of a planning scenario. Node 0 is the depot and nodes
 * 1..size()-1 are the stops in input order. The matrix is the only way the algorithms see the
 * road network, which keeps this package independent of any routing engine.
 */
public interface DirectedCostMatrix {

    int size();

    /** Directed road distance in metres from node {@code from} to node {@code to}. */
    double distance(int from, int to);

    /** Directed base travel time in seconds from node {@code from} to node {@code to}. */
    double travelTime(int from, int to);
}
