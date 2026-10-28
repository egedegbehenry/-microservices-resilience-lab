package com.henry.resilience.availability.lab;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
@Service @Profile("lab")
public class LabOperation {
    private final FaultPlan plan;
    
    public LabOperation(FaultPlan plan) {
        this.plan=plan; 
    }
    @RateLimiter(name="dependencyCircuitBreaker")
    @CircuitBreaker(name="dependencyCircuitBreaker")
    @Retry(name="dependencyCircuitBreaker")
    public String execute() { plan.invoke(); return "AVAILABLE"; }
}
