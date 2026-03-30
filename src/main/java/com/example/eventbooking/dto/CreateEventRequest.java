package com.example.eventbooking.dto;

import java.time.LocalDateTime;

public record CreateEventRequest(
        String title,
        String description,
        LocalDateTime eventDate,
        String location,
        Integer maxSeats
) {
}
