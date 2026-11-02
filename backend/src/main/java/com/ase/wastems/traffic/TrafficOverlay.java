package com.ase.wastems.traffic;

/** The overlay served to the map: a snapshot with its age, or the reason it is unavailable. */
public record TrafficOverlay(boolean available, String unavailableReason, TrafficSnapshot snapshot, Long ageMinutes,
        int staleAfterMinutes) {}
