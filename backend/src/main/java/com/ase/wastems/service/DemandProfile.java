package com.ase.wastems.service;

import java.util.Random;

/** Synthetic demand profiles of the seeded MN01 experiment generator (D-050). Whole kilograms. */
public enum DemandProfile {
    UNIFORM_LOW(1),
    UNIFORM_HIGH(2),
    HOTSPOT(3);

    private final int code;

    DemandProfile(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    /** Draws the demand of the next point from the profile's random stream. */
    public int drawKg(Random dem) {
        return switch (this) {
            case UNIFORM_LOW -> 10 + dem.nextInt(21);
            case UNIFORM_HIGH -> 30 + dem.nextInt(31);
            case HOTSPOT -> dem.nextInt(5) == 0 ? 60 + dem.nextInt(61) : 5 + dem.nextInt(16);
        };
    }
}
