package com.ase.wastems.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the MN01 demonstration load (wastems.demo.*). `reset` is a one-shot switch: it
 * wipes the demonstration data before the load runs again (D-055) and is meant to be given on
 * the command line (`--wastems.demo.reset=true`), never committed as {@code true}.
 */
@ConfigurationProperties(prefix = "wastems.demo")
public record DemoProperties(boolean enabled, boolean reset) {}
