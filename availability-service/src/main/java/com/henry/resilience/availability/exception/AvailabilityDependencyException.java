
package com.henry.resilience.availability.exception;

public class AvailabilityDependencyException
        extends RuntimeException {

    public AvailabilityDependencyException(String message) {
        super(message);
    }
}
