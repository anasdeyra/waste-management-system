package com.ase.wastems.routing;

/** The road graph cannot be used at all (missing or corrupt snapshot, failed import). Maps to 503. */
public class RoutingUnavailableException extends RuntimeException {

    public RoutingUnavailableException(String message) {
        super(message);
    }

    public RoutingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
