package com.example.eventbooking.service;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.exception.AccessDeniedException;
import com.example.eventbooking.exception.EventNotFoundException;
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
        return EventMapper.toResponse(eventRepository.save(event));
    }

    public List<EventResponse> getMyEvents(Long organizerId) {
        return eventRepository.findByOrganizerId(organizerId).stream()
                .map(EventMapper::toResponse)
                .toList();
    }

    public List<EventResponse> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDateTime.now()).stream()
                .map(EventMapper::toResponse)
                .toList();
    }

    public EventResponse getEventById(Long id) {
        return EventMapper.toResponse(findOrThrow(id));
    }

    public EventResponse updateEvent(Long id, CreateEventRequest request, Long organizerId) {
        Event event = findOrThrow(id);
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Only the organizer can update this event");
        }
        validateCreateEventRequest(request);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setLocation(request.location());
        event.setMaxSeats(request.maxSeats());
        return EventMapper.toResponse(eventRepository.save(event));
    }

    public void deleteEvent(Long id, Long organizerId) {
        Event event = findOrThrow(id);
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Only the organizer can delete this event");
        }
        eventRepository.delete(event);
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

    private Event findOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + id));
    }
}
