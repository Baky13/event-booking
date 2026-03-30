package com.example.eventbooking.controller;

import com.example.eventbooking.dto.BookingResponse;
import com.example.eventbooking.security.UserPrincipal;
import com.example.eventbooking.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/api/events/{id}/book")
    public ResponseEntity<BookingResponse> bookEvent(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.bookEvent(id, principal.getId()));
    }

    // Баг 1 исправлен: {id} = eventId, сервис сам находит бронирование пользователя
    @DeleteMapping("/api/events/{id}/book")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        bookingService.cancelBookingByEventId(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/events/{id}/waitlist")
    public ResponseEntity<BookingResponse> joinWaitlist(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bookingService.joinWaitlist(id, principal.getId()));
    }

    @DeleteMapping("/api/events/{id}/waitlist")
    public ResponseEntity<Void> leaveWaitlist(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        bookingService.leaveWaitlist(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/bookings/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bookingService.getMyBookings(principal.getId()));
    }
}
