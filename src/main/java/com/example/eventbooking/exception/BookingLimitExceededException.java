package com.example.eventbooking.exception;

public class BookingLimitExceededException extends RuntimeException {
    public BookingLimitExceededException(String message) { super(message); }
}
