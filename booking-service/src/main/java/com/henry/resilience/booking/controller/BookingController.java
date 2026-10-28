
package com.henry.resilience.booking.controller;

import com.henry.resilience.booking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController tells Spring this is a REST API controller
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/availability")
    public ResponseEntity<String> checkAvailability() {
        String result = bookingService.checkAvailability();
        return ResponseEntity.ok(result);
    }

}
