package com.ase.wastems.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bounding box (WGS84, degrees) of the supported service area; used to catch swapped coordinates. */
@ConfigurationProperties(prefix = "wastems.service-area")
public record ServiceAreaProperties(
        double minLongitude, double maxLongitude, double minLatitude, double maxLatitude) {

    /** Inclusive bounds. */
    public boolean contains(double longitude, double latitude) {
        return longitude >= minLongitude
                && longitude <= maxLongitude
                && latitude >= minLatitude
                && latitude <= maxLatitude;
    }
}
