
package com.henry.resilience.availability.service;

import com.henry.resilience.availability.exception.AvailabilityDependencyException;
import org.springframework.stereotype.Service;

@Service
public class AvailabilityService {

    // Scenario 1: Normal successful response
    public String checkAvailability() {
        return "AVAILABLE";
    }

    // Scenario 2: Simulated dependency failure

    public String simulateFailure() {
        throw new AvailabilityDependencyException(
                "Simulated availability dependency failure"
        );
    }

    // Scenario 3: Simulated slow response
    public String simulateDelay() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Simulated delay interrupted", e
            );
        }

        return "AVAILABLE";
    }
}