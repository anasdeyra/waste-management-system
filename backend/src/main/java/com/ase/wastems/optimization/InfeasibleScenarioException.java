package com.ase.wastems.optimization;

/** Raised by a construction method that cannot cover every stop under the scenario constraints. */
public class InfeasibleScenarioException extends RuntimeException {

    public InfeasibleScenarioException(String message) {
        super(message);
    }
}
