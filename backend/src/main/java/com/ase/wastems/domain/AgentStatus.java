package com.ase.wastems.domain;

/** Availability of an agent for tour assignment. */
public enum AgentStatus {
    /** The agent can be assigned to a tour. */
    AVAILABLE,
    /** The agent is already assigned to a tour on the planning day. */
    ASSIGNED,
    /** The agent is not available (leave, training, ...). */
    UNAVAILABLE
}
