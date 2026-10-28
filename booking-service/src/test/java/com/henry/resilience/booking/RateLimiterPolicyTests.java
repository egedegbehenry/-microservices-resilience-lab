package com.henry.resilience.booking;
import java.time.Duration;
import io.github.resilience4j.ratelimiter.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class RateLimiterPolicyTests {
 @ParameterizedTest @ValueSource(ints={5,10,20})
 void exactQuotaThenImmediateRejection(int quota) {
   var limiter=RateLimiter.of("quota",RateLimiterConfig.custom().limitForPeriod(quota)
       .limitRefreshPeriod(Duration.ofSeconds(1)).timeoutDuration(Duration.ZERO).build());
   for(int i=0;i<quota;i++) assertTrue(limiter.acquirePermission());
   assertFalse(limiter.acquirePermission());
 }
 @Test void waitsForNextRefreshWhenTimeoutAllows() {
   var limiter=RateLimiter.of("waiting",RateLimiterConfig.custom().limitForPeriod(1)
       .limitRefreshPeriod(Duration.ofMillis(100)).timeoutDuration(Duration.ofMillis(500)).build());
   assertTrue(limiter.acquirePermission());
   long start=System.nanoTime(); assertTrue(limiter.acquirePermission());
   assertTrue(Duration.ofNanos(System.nanoTime()-start).toMillis()>=20);
 }
 @Test void insufficientTimeoutRejects() {
   var limiter=RateLimiter.of("timeout",RateLimiterConfig.custom().limitForPeriod(1)
       .limitRefreshPeriod(Duration.ofHours(1)).timeoutDuration(Duration.ofMillis(1)).build());
   assertTrue(limiter.acquirePermission()); assertFalse(limiter.acquirePermission());
 }
}
