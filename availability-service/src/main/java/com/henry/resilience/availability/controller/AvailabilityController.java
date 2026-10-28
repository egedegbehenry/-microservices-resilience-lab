
package com.henry.resilience.availability.controller;

import com.henry.resilience.availability.service.AvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Availability service is running");
    }

    @GetMapping("/check")
    public ResponseEntity<String> checkAvailability() {
        return ResponseEntity.ok(availabilityService.checkAvailability());
    }

}
