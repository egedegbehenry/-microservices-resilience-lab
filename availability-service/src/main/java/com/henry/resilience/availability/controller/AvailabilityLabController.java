package com.henry.resilience.availability.controller;
import com.henry.resilience.availability.service.AvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @org.springframework.context.annotation.Profile("lab")
@RequestMapping("/api/availability")
public class AvailabilityLabController {
    private final AvailabilityService availabilityService;
    public AvailabilityLabController(AvailabilityService service) { this.availabilityService=service; }
    // Scenario 2: Simulate a dependency failure
    @GetMapping("/simulate/failure")
    public ResponseEntity<String> simulateFailure() {
        return ResponseEntity.ok(
                availabilityService.simulateFailure()
        );
    }

    // Scenario 3: Simulate a slow dependency
    @GetMapping("/simulate/delay")
    public ResponseEntity<String> simulateDelay() {
        return ResponseEntity.ok(
                availabilityService.simulateDelay()
        );
    }


}
