package com.ase.wastems.traffic;

import java.time.Instant;
import java.util.List;

/** One drawn sensor link; coordinates are {longitude, latitude} (GeoJSON order). */
public record TrafficLink(String linkId, String name, String borough, String owner, TrafficLinkStatus status,
        Double speedMph, Integer travelTimeS, SpeedBand speedBand, Instant dataAsOf, String dataAsOfLocal,
        GeometrySource geometrySource, boolean geometryTruncated, List<double[]> coordinates) {}
