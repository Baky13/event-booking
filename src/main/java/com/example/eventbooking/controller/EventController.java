package com.example.eventbooking.controller;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.security.UserPrincipal;
import com.example.eventbooking.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(request, principal.getId()));
    }

    // Пагинация: GET /api/events?page=0&size=20
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getUpcomingEvents(
            @PageableDefault(size = 20, sort = "eventDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(eventService.getUpcomingEventsPaged(pageable));
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
                                                     @Valid @RequestBody CreateEventRequest request,
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
