package com.ase.wastems.routing;

import java.util.List;

/** One directed road leg: distance (m), base travel time (s) and road-following geometry. */
public record RoadLeg(double distanceM, double travelTimeS, List<WgsPoint> geometry) {
    public RoadLeg {
        geometry = List.copyOf(geometry);
    }
}
