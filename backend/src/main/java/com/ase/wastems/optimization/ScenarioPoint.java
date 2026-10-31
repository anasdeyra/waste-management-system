package com.ase.wastems.optimization;

/**
 * A point that has to be visited by a tour.
 *
 * @param id identifier of the underlying collection point
 * @param x planning coordinate on the x axis, in kilometres
 * @param y planning coordinate on the y axis, in kilometres
 * @param demandKg kilograms of waste to collect at this point
 */
public record ScenarioPoint(Long id, double x, double y, double demandKg) {

    public double distanceTo(ScenarioPoint other) {
        return DistanceCalculator.euclidean(x, y, other.x, other.y);
    }
}
