package com.henry.resilience.availability.exception;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
@RestControllerAdvice
public class ResilienceExceptionHandler {
    private final String retryAfter;
    public ResilienceExceptionHandler(io.github.resilience4j.ratelimiter.RateLimiterRegistry registry) {
        long milliseconds=registry.rateLimiter("dependencyCircuitBreaker").getRateLimiterConfig().getLimitRefreshPeriod().toMillis();
        this.retryAfter=Long.toString(Math.max(1,(milliseconds+999)/1000));
    }
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<?> open(CallNotPermittedException e) { return error(503,"CIRCUIT_OPEN",e.getMessage()); }
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<?> limited(RequestNotPermitted e) { return error(429,"RATE_LIMITED",e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException e) { return error(400,"INVALID_CONFIG",e.getMessage()); }
    
    private ResponseEntity<?> error(int status,String code,String message) {
        var builder=ResponseEntity.status(status);
        if(status==429) builder.header("Retry-After",retryAfter);
        return builder.body(Map.of("status",status,"code",code,"message",message==null?code:message));
    }
}
