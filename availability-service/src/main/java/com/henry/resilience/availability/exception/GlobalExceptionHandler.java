
package com.henry.resilience.availability.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AvailabilityDependencyException.class)
    public ResponseEntity<ApiErrorResponse> handleDependencyException(
            AvailabilityDependencyException exception) {

        ApiErrorResponse error = new ApiErrorResponse(
                503,
                "Service Unavailable",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }

    public record ApiErrorResponse(
            int status,
            String error,
            String message
    ) {}
}
