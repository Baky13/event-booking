package com.example.eventbooking.service;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.exception.ValidationException;
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
        // Валидация
        if (request.eventDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Event date must be in the future");
        }
        if (request.maxSeats() < 1) {
            throw new ValidationException("Max seats must be at least 1");
        }

        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setLocation(request.location());
        event.setMaxSeats(request.maxSeats());
        event.setAvailableSeats(request.maxSeats());
        event.setOrganizerId(organizerId);
        event.setCreatedAt(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);

        return mapToResponse(savedEvent);
    }

    public List<EventResponse> getMyEvents(Long organizerId) {
        List<Event> events = eventRepository.findByOrganizerId(organizerId);
        return events.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<EventResponse> getUpcomingEvents() {
        List<Event> events = eventRepository.findUpcomingEvents(LocalDateTime.now());
        return events.stream()
                .map(this::mapToResponse)
                .toList();
    }

    private EventResponse mapToResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getMaxSeats(),
                event.getAvailableSeats(),
                event.getOrganizerId(),
                event.getCreatedAt()
        );
    }
}
