package com.ase.wastems.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the versioned road snapshot and the embedded router (wastems.routing.*). */
@ConfigurationProperties(prefix = "wastems.routing")
public record RoutingProperties(
        String osmLocation, String expectedSha256, String versionPrefix, String cacheDir,
        double maxSnapDistanceMeters) {}
