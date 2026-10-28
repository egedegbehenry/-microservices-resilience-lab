package com.henry.resilience.booking.lab;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
@Service @Profile("lab")
public class LabOperation {
    private final FaultPlan plan;
    private final org.springframework.web.client.RestClient client;
    public LabOperation(FaultPlan plan, @org.springframework.beans.factory.annotation.Qualifier("availabilityRestClient") org.springframework.web.client.RestClient client) {
        this.plan=plan; this.client=client;
    }
    @RateLimiter(name="availabilityCircuitBreaker")
    @CircuitBreaker(name="availabilityCircuitBreaker")
    @Retry(name="availabilityCircuitBreaker")
    public String execute() { plan.invoke(); return client.get().uri("/lab/operation").retrieve().body(String.class); }
}
