package com.example.eventbooking.exception;

public class DuplicateBookingException extends RuntimeException {
    public DuplicateBookingException(String message) { super(message); }
}
