package com.example.eventbooking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description,

        @NotNull(message = "Event date is required")
        @Future(message = "Event date must be in the future")
        LocalDateTime eventDate,

        @NotBlank(message = "Location is required")
        @Size(max = 300, message = "Location must not exceed 300 characters")
        String location,

        @NotNull(message = "Max seats is required")
        @Min(value = 1, message = "Max seats must be at least 1")
        Integer maxSeats
) {}
