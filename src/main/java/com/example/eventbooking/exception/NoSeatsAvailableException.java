package com.example.eventbooking.exception;

public class NoSeatsAvailableException extends RuntimeException {
    public NoSeatsAvailableException(String message) { super(message); }
}
