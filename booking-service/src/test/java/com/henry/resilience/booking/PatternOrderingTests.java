package com.henry.resilience.booking;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PatternOrderingTests {
    @Test void breakerOutsideRetryRecordsOneOutcomeWhileRetryOutsideRecordsEachAttempt() {
        var config = CircuitBreakerConfig.custom().slidingWindowSize(4).minimumNumberOfCalls(4).build();
        var retry = Retry.of("ordering", RetryConfig.custom().maxAttempts(3).waitDuration(Duration.ofMillis(1))
                .retryExceptions(IllegalStateException.class).build());
        var calls = new AtomicInteger();
        Supplier<String> failing = () -> { calls.incrementAndGet(); throw new IllegalStateException("failure"); };
        var logical = CircuitBreaker.of("logical", config);
        var perAttempt = CircuitBreaker.of("attempts", config);
        var breakerOutside = CircuitBreaker.decorateSupplier(logical, Retry.decorateSupplier(retry, failing));
        var retryOutside = Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(perAttempt, failing));
        assertThrows(IllegalStateException.class, breakerOutside::get);
        assertEquals(3, calls.get());
        assertEquals(1, logical.getMetrics().getNumberOfFailedCalls());
        assertThrows(IllegalStateException.class, retryOutside::get);
        assertEquals(6, calls.get());
        assertEquals(3, perAttempt.getMetrics().getNumberOfFailedCalls());
    }
}
