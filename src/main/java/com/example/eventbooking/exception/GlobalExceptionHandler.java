package com.example.eventbooking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidation(ValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEventNotFound(EventNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(NoSeatsAvailableException.class)
    public ResponseEntity<Map<String, String>> handleNoSeats(NoSeatsAvailableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateBookingException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateBookingException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EventExpiredException.class)
    public ResponseEntity<Map<String, String>> handleExpired(EventExpiredException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BookingLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleLimit(BookingLimitExceededException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(CancellationDeadlineException.class)
    public ResponseEntity<Map<String, String>> handleDeadline(CancellationDeadlineException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccess(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }
}
