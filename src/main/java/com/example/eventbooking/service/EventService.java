package com.example.eventbooking.service;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.repository.EventRepository;

import java.util.List;

public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public EventResponse createEvent(CreateEventRequest request, Long organizerId) {
        // Будет реализовано в GREEN фазе
        return null;
    }

    public List<EventResponse> getMyEvents(Long organizerId) {
        // Будет реализовано в GREEN фазе
        return null;
    }

    public List<EventResponse> getUpcomingEvents() {
        // Будет реализовано в GREEN фазе
        return null;
    }
}
