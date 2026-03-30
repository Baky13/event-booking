package com.example.eventbooking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        LocalDateTime eventDate,

        @NotBlank(message = "Location is required")
        String location,

        @NotNull(message = "Max seats is required")
        @Min(value = 1, message = "Max seats must be at least 1")
        Integer maxSeats
) {}
