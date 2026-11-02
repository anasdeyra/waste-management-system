package com.ase.wastems.traffic;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** NYC DOT traffic overlay settings (US-34, D-021, D-050): committed snapshot, display only. */
@ConfigurationProperties(prefix = "wastems.traffic")
public record TrafficProperties(String snapshotLocation, int staleAfterMinutes, double maxPlausibleSpeedMph) {}
