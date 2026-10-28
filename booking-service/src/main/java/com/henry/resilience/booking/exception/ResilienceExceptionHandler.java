package com.henry.resilience.booking.exception;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
@RestControllerAdvice
public class ResilienceExceptionHandler {
    private final String retryAfter;
    public ResilienceExceptionHandler(io.github.resilience4j.ratelimiter.RateLimiterRegistry registry) {
        long milliseconds=registry.rateLimiter("availabilityCircuitBreaker").getRateLimiterConfig().getLimitRefreshPeriod().toMillis();
        this.retryAfter=Long.toString(Math.max(1,(milliseconds+999)/1000));
    }
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<?> open(CallNotPermittedException e) { return error(503,"CIRCUIT_OPEN",e.getMessage()); }
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<?> limited(RequestNotPermitted e) { return error(429,"RATE_LIMITED",e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException e) { return error(400,"INVALID_CONFIG",e.getMessage()); }
    @ExceptionHandler(org.springframework.web.client.RestClientResponseException.class)
    public ResponseEntity<?> downstream(org.springframework.web.client.RestClientResponseException e) {
        if (e.getStatusCode().value()==429) {
            String after=e.getResponseHeaders()==null?null:e.getResponseHeaders().getFirst("Retry-After");
            return ResponseEntity.status(429).header("Retry-After",after==null?retryAfter:after)
                .body(Map.of("status",429,"code","DOWNSTREAM_RATE_LIMITED","message",e.getResponseBodyAsString()));
        }
        return error(e.getStatusCode().value(), "DOWNSTREAM_HTTP_ERROR",
                e.getResponseBodyAsString().isBlank() ? e.getMessage() : e.getResponseBodyAsString());
    }
    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<?> timeout(org.springframework.web.client.ResourceAccessException e) { return error(504,"DOWNSTREAM_TIMEOUT",e.getMessage()); }
    private ResponseEntity<?> error(int status,String code,String message) {
        var builder=ResponseEntity.status(status);
        if(status==429) builder.header("Retry-After",retryAfter);
        return builder.body(Map.of("status",status,"code",code,"message",message==null?code:message));
    }
}
