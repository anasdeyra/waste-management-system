package com.ase.wastems.routing;

/** Directed road routing on one versioned road graph. */
public interface RoadRouter {

    /** Version ID of the loaded snapshot, e.g. osm-mn01-e1f2fabaa937 (initializes the graph). */
    String roadGraphVersion();

    String routingEngine();

    /**
     * Snap distance in metres; throws RoadLegUnavailableException(POINT_NOT_ROUTABLE) when the
     * point does not snap or snaps farther than the configured maximum.
     */
    double snapDistanceMeters(WgsPoint point);

    /** Best directed car path; throws RoadLegUnavailableException(NO_ROAD_PATH) on a routing error. */
    RoadLeg route(WgsPoint from, WgsPoint to);
}
