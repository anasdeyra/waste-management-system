package com.ase.wastems.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the MN01 demonstration load (wastems.demo.*). */
@ConfigurationProperties(prefix = "wastems.demo")
public record DemoProperties(boolean enabled) {}
