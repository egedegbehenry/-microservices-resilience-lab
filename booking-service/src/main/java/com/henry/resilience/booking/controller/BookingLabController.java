package com.henry.resilience.booking.controller;
import com.henry.resilience.booking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @org.springframework.context.annotation.Profile("lab")
@RequestMapping("/api/bookings")
public class BookingLabController {
    private final BookingService bookingService;
    public BookingLabController(BookingService service) { this.bookingService=service; }
    // Test downstream delay and HTTP read timeout
    @GetMapping("/availability/delay")
    public ResponseEntity<String> checkDelayedAvailability() {
        String result = bookingService.checkDelayedAvailability();
        return ResponseEntity.ok(result);
    }

    // Scenario 3: Simulate downstream failure
    @GetMapping("/availability/failure")
    public ResponseEntity<String> checkFailingAvailability() {
        String result = bookingService.checkFailingAvailability();
        return ResponseEntity.ok(result);
    }


}
