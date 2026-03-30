package com.example.eventbooking.controller;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.exception.EventNotFoundException;
import com.example.eventbooking.security.UserPrincipal;
import com.example.eventbooking.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<EventResponse> createEvent(@RequestBody CreateEventRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        EventResponse response = eventService.createEvent(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    @GetMapping("/my")
    public ResponseEntity<List<EventResponse>> getMyEvents(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.getMyEvents(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id,
                                                     @RequestBody CreateEventRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        eventService.deleteEvent(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
