package com.ase.wastems.routing;

/** A required directed leg or point cannot be routed on the snapshot (US-22). */
public class RoadLegUnavailableException extends RuntimeException {

    public enum Reason { POINT_NOT_ROUTABLE, NO_ROAD_PATH }

    private final Reason reason;
    private final int fromNode;
    private final int toNode;

    public RoadLegUnavailableException(Reason reason, String message) {
        this(reason, -1, -1, message);
    }

    /** Node indices are -1 when unknown or not applicable. */
    public RoadLegUnavailableException(Reason reason, int fromNode, int toNode, String message) {
        super(message);
        this.reason = reason;
        this.fromNode = fromNode;
        this.toNode = toNode;
    }

    public Reason getReason() {
        return reason;
    }

    public int getFromNode() {
        return fromNode;
    }

    public int getToNode() {
        return toNode;
    }
}
