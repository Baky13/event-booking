package com.example.eventbooking.controller;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@RequestBody CreateEventRequest request) {
        // Будет реализовано в GREEN фазе
        return null;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {
        // Будет реализовано в GREEN фазе
        return null;
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventResponse>> getMyEvents() {
        // Будет реализовано в GREEN фазе
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        // Будет реализовано в GREEN фазе
        return null;
    }
}
