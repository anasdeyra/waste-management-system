package com.ase.wastems.domain;

/** Role an agent plays inside a collection crew. */
public enum AgentRole {
    /** The agent is responsible for the vehicle and must be assigned to every tour. */
    DRIVER,
    /** The agent supports the driver; a tour accepts zero to two collectors. */
    COLLECTOR
}
