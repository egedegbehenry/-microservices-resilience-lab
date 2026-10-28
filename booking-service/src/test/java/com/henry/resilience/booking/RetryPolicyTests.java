package com.henry.resilience.booking;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTests {
    @Test void exponentialBackoffHasConfiguredIntervalsAndRecovers() {
        var retry = Retry.of("backoff", RetryConfig.custom().maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(10, 2))
                .retryExceptions(IllegalStateException.class).build());
        var intervals = new ArrayList<Long>();
        retry.getEventPublisher().onRetry(event -> intervals.add(event.getWaitInterval().toMillis()));
        var attempts = new AtomicInteger();
        String result = Retry.decorateSupplier(retry, () -> {
            if (attempts.incrementAndGet() < 3) throw new IllegalStateException("temporary");
            return "AVAILABLE";
        }).get();
        assertEquals("AVAILABLE", result);
        assertEquals(3, attempts.get());
        assertEquals(List.of(10L, 20L), intervals);
    }

    @Test void configuredAttemptLimitIsRespectedOnExhaustion() {
        var retry = Retry.of("two", RetryConfig.custom().maxAttempts(2)
                .intervalFunction(IntervalFunction.of(1)).build());
        var attempts = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> Retry.decorateSupplier(retry, () -> {
            attempts.incrementAndGet(); throw new IllegalStateException("persistent");
        }).get());
        assertEquals(2, attempts.get());
    }
}
