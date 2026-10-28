
package com.henry.resilience.booking.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BookingService {

    private final RestClient availabilityRestClient;

    public BookingService(
            @Qualifier("availabilityRestClient") RestClient availabilityRestClient) {
        this.availabilityRestClient = availabilityRestClient;
    }

    //All three @CircuitBreaker annotation named availabilityCircuitBreaker,
    // which will allow us to monitor successful and failed downstream calls together.

    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "availabilityCircuitBreaker")
    @CircuitBreaker(name = "availabilityCircuitBreaker")
    @io.github.resilience4j.retry.annotation.Retry(name = "availabilityCircuitBreaker")
    public String checkAvailability() {
        return availabilityRestClient
                .get()
                .uri("/api/availability/check")
                .retrieve()
                .body(String.class);
    }

    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "availabilityCircuitBreaker")
    @CircuitBreaker(name = "availabilityCircuitBreaker")
    @io.github.resilience4j.retry.annotation.Retry(name = "availabilityCircuitBreaker")
    public String checkDelayedAvailability() {
        return availabilityRestClient
                .get()
                .uri("/api/availability/simulate/delay")
                .retrieve()
                .body(String.class);
    }

    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(name = "availabilityCircuitBreaker")
    @CircuitBreaker(name = "availabilityCircuitBreaker")
    @io.github.resilience4j.retry.annotation.Retry(name = "availabilityCircuitBreaker")
    public String checkFailingAvailability() {
        return availabilityRestClient
                .get()
                .uri("/api/availability/simulate/failure")
                .retrieve()
                .body(String.class);
    }
}
