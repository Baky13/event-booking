package com.example.eventbooking.service;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.exception.AccessDeniedException;
import com.example.eventbooking.exception.EventNotFoundException;
import com.example.eventbooking.exception.ValidationException;
import com.example.eventbooking.mapper.EventMapper;
import com.example.eventbooking.model.Event;
import com.example.eventbooking.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request, Long organizerId) {
        validateCreateEventRequest(request);
        Event event = buildEventFromRequest(request, organizerId);
        log.info("Organizer {} created event '{}'", organizerId, request.title());
        return EventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(Long organizerId) {
        return eventRepository.findByOrganizerId(organizerId).stream()
                .map(EventMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDateTime.now()).stream()
                .map(EventMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getUpcomingEventsPaged(Pageable pageable) {
        return eventRepository.findUpcomingEventsPaged(LocalDateTime.now(), pageable)
                .map(EventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        return EventMapper.toResponse(findOrThrow(id));
    }

    // Метод для проверки что организатор не бронирует своё мероприятие (используется в тесте)
    public void bookOwnEvent(Long eventId, Long userId) {
        Event event = findOrThrow(eventId);
        if (event.getOrganizerId().equals(userId)) {
            throw new ValidationException("Event organizer cannot book their own event");
        }
    }

    @Transactional
    public EventResponse updateEvent(Long id, CreateEventRequest request, Long organizerId) {
        Event event = findOrThrow(id);
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Only the organizer can update this event");
        }
        validateCreateEventRequest(request);

        if (!request.maxSeats().equals(event.getMaxSeats())) {
            int diff = request.maxSeats() - event.getMaxSeats();
            int newAvailable = event.getAvailableSeats() + diff;
            if (newAvailable < 0) {
                throw new ValidationException("Cannot reduce capacity below booked count");
            }
            event.setMaxSeats(request.maxSeats());
            event.setAvailableSeats(newAvailable);
        }

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setLocation(request.location());
        log.info("Organizer {} updated event {}", organizerId, id);
        return EventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long id, Long organizerId) {
        Event event = findOrThrow(id);
        if (!event.getOrganizerId().equals(organizerId)) {
            throw new AccessDeniedException("Only the organizer can delete this event");
        }
        log.info("Organizer {} deleted event {}", organizerId, id);
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
