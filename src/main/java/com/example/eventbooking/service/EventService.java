package com.example.eventbooking.service;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.exception.ValidationException;
import com.example.eventbooking.mapper.EventMapper;
import com.example.eventbooking.model.Event;
import com.example.eventbooking.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public EventResponse createEvent(CreateEventRequest request, Long organizerId) {
        validateCreateEventRequest(request);
        
        Event event = buildEventFromRequest(request, organizerId);
        Event savedEvent = eventRepository.save(event);
        
        return EventMapper.toResponse(savedEvent);
    }

    public List<EventResponse> getMyEvents(Long organizerId) {
        List<Event> events = eventRepository.findByOrganizerId(organizerId);
        return events.stream()
                .map(EventMapper::toResponse)
                .toList();
    }

    public List<EventResponse> getUpcomingEvents() {
        List<Event> events = eventRepository.findUpcomingEvents(LocalDateTime.now());
        return events.stream()
                .map(EventMapper::toResponse)
                .toList();
    }

    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return EventMapper.toResponse(event);
    }

    private void validateCreateEventRequest(CreateEventRequest request) {
        if (request.eventDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Event date must be in the future");
        }
        if (request.maxSeats() < 1) {
            throw new ValidationException("Max seats must be at least 1");
        }
    }

    private Event buildEventFromRequest(CreateEventRequest request, Long organizerId) {
        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setLocation(request.location());
        event.setMaxSeats(request.maxSeats());
        event.setAvailableSeats(request.maxSeats());
        event.setOrganizerId(organizerId);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}
