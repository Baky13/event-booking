package com.example.eventbooking.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String message) { super(message); }
}
