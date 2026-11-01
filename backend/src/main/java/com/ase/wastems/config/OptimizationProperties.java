package com.ase.wastems.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the three-method evaluation (wastems.optimization.*). */
@ConfigurationProperties(prefix = "wastems.optimization")
public record OptimizationProperties(double serviceTimeSeconds) {}
