package com.example.eventbooking.mapper;

import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.model.Event;

public class EventMapper {

    public static EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getMaxSeats(),
                event.getAvailableSeats(),
                event.getCreatedAt()
        );
    }
}
