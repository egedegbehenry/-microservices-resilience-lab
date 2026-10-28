package com.henry.resilience.booking.lab;
import java.util.*;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
@RestController @Profile("lab") @RequestMapping("/lab")
public class LabController {
    private final java.util.concurrent.atomic.AtomicLong stateTransitions = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong retryAttempts = new java.util.concurrent.atomic.AtomicLong();
    private final FaultPlan plan;
    private final LabOperation operation;
    private final CircuitBreaker breaker;
    private final Deque<String> events = new java.util.concurrent.ConcurrentLinkedDeque<>();
    public LabController(FaultPlan plan, LabOperation operation, CircuitBreakerRegistry breakers, RetryRegistry retries, RateLimiterRegistry limits) {
        this.plan=plan; this.operation=operation; this.breaker=breakers.circuitBreaker("availabilityCircuitBreaker");
        breaker.getEventPublisher().onStateTransition(e -> stateTransitions.incrementAndGet());
        breaker.getEventPublisher().onEvent(e -> record(e.toString()));
        retries.retry("availabilityCircuitBreaker").getEventPublisher().onRetry(e -> retryAttempts.incrementAndGet());
        retries.retry("availabilityCircuitBreaker").getEventPublisher().onEvent(e -> record(e.toString()));
        limits.rateLimiter("availabilityCircuitBreaker").getEventPublisher().onFailure(e -> record(e.toString()));
    }
    private synchronized void record(String value) {
        events.addLast(value); while(events.size()>100) events.pollFirst();
        org.slf4j.LoggerFactory.getLogger(getClass()).info("event=resilience detail={}",value);
    }
    @GetMapping("/operation") public String operation() { return operation.execute(); }
    @PostMapping("/config") public Map<String,Object> configure(@RequestBody FaultPlan.Plan config) { plan.configure(config); return state(); }
    @PostMapping("/reset") public Map<String,Object> reset() { breaker.reset(); plan.configure(new FaultPlan.Plan("success",0,0)); events.clear(); stateTransitions.set(0); retryAttempts.set(0); return state(); }
    @GetMapping("/state") public Map<String,Object> state() {
        return Map.of("breaker",breaker.getName(),"state",breaker.getState().name(),"attempts",plan.attempts(),
            "stateTransitions",stateTransitions.get(),"retryAttempts",retryAttempts.get(),
            "bufferedCalls",breaker.getMetrics().getNumberOfBufferedCalls(),"failedCalls",breaker.getMetrics().getNumberOfFailedCalls(),
            "notPermittedCalls",breaker.getMetrics().getNumberOfNotPermittedCalls(),"plan",plan.plan(),"events",List.copyOf(events));
    }
}
