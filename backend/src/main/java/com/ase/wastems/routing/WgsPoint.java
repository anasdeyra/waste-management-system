package com.ase.wastems.routing;

/** A WGS84 position with explicit field order (docs/geospatial-data.md §4). */
public record WgsPoint(double longitude, double latitude) {}
