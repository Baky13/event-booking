package com.example.eventbooking.controller;

import com.example.eventbooking.config.SecurityConfig;
import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.exception.EventNotFoundException;
import com.example.eventbooking.exception.ValidationException;
import com.example.eventbooking.security.JwtTokenProvider;
import com.example.eventbooking.security.UserPrincipal;
import com.example.eventbooking.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private UsernamePasswordAuthenticationToken userAuth() {
        UserPrincipal principal = new UserPrincipal(1L, "user@test.com");
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }

    @Test
    void createEvent_ValidData_ReturnsCreated() throws Exception {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Java Meetup", "Description", LocalDateTime.now().plusDays(1), "Office", 10);
        EventResponse response = eventResponse(1L, "Java Meetup", 10, 10, 1L);
        when(eventService.createEvent(any(), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/events")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Java Meetup"))
                .andExpect(jsonPath("$.maxSeats").value(10))
                .andExpect(jsonPath("$.availableSeats").value(10));
    }

    @Test
    void createEvent_Unauthorized_Returns401() throws Exception {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Java Meetup", "Description", LocalDateTime.now().plusDays(1), "Office", 10);

        // When & Then
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createEvent_DateInPast_Returns400() throws Exception {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Past Event", "Description", LocalDateTime.now().plusDays(1), "Office", 10);
        when(eventService.createEvent(any(), any())).thenThrow(new ValidationException("Event date must be in the future"));

        // When & Then
        mockMvc.perform(post("/api/events")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Event date must be in the future"));
    }

    @Test
    void createEvent_ZeroCapacity_Returns400() throws Exception {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Event", "Description", LocalDateTime.now().plusDays(1), "Office", 0);
        when(eventService.createEvent(any(), any())).thenThrow(new ValidationException("Max seats must be at least 1"));

        // When & Then
        mockMvc.perform(post("/api/events")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Max seats must be at least 1"));
    }

    @Test
    void getUpcomingEvents_Returns200() throws Exception {
        // Given
        List<EventResponse> events = List.of(
                eventResponse(1L, "Event 1", 10, 10, 1L),
                eventResponse(2L, "Event 2", 5, 5, 2L)
        );
        when(eventService.getUpcomingEvents()).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Event 1"))
                .andExpect(jsonPath("$[1].title").value("Event 2"));
    }

    @Test
    void getMyEvents_Returns200() throws Exception {
        // Given
        List<EventResponse> events = List.of(eventResponse(1L, "My Event", 10, 10, 1L));
        when(eventService.getMyEvents(any())).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/events/my").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("My Event"));
    }

    @Test
    void getMyEvents_Unauthorized_Returns401() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/events/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEventById_Exists_Returns200() throws Exception {
        // Given
        EventResponse event = eventResponse(1L, "Event", 10, 10, 1L);
        when(eventService.getEventById(1L)).thenReturn(event);

        // When & Then
        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Event"));
    }

    @Test
    void getEventById_NotFound_Returns404() throws Exception {
        // Given
        when(eventService.getEventById(999L)).thenThrow(new EventNotFoundException("Event not found: 999"));

        // When & Then
        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Event not found: 999"));
    }

    @Test
    void getEventById_NoAuth_Returns200() throws Exception {
        // Given — публичный эндпоинт, авторизация не нужна
        EventResponse event = eventResponse(1L, "Event", 10, 10, 1L);
        when(eventService.getEventById(1L)).thenReturn(event);

        // When & Then
        mockMvc.perform(get("/api/events/1")) // без authentication
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateEvent_ValidData_Returns200() throws Exception {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Updated", "Desc", LocalDateTime.now().plusDays(2), "Office", 20);
        EventResponse response = eventResponse(1L, "Updated", 20, 17, 1L);
        when(eventService.updateEvent(any(), any(), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(put("/api/events/1")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.maxSeats").value(20));
    }

    @Test
    void updateEvent_NotOrganizer_Returns403() throws Exception {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Updated", "Desc", LocalDateTime.now().plusDays(2), "Office", 10);
        when(eventService.updateEvent(any(), any(), any()))
                .thenThrow(new com.example.eventbooking.exception.AccessDeniedException("Only the organizer can update this event"));

        // When & Then
        mockMvc.perform(put("/api/events/1")
                        .with(authentication(userAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEvent_Organizer_Returns204() throws Exception {
        // Given
        doNothing().when(eventService).deleteEvent(any(), any());

        // When & Then
        mockMvc.perform(delete("/api/events/1").with(authentication(userAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEvent_NotOrganizer_Returns403() throws Exception {
        // Given
        doThrow(new com.example.eventbooking.exception.AccessDeniedException("Only the organizer can delete this event"))
                .when(eventService).deleteEvent(any(), any());

        // When & Then
        mockMvc.perform(delete("/api/events/1").with(authentication(userAuth())))
                .andExpect(status().isForbidden());
    }

    private EventResponse eventResponse(Long id, String title, int maxSeats, int availableSeats, Long organizerId) {
        return new EventResponse(id, title, "Description", LocalDateTime.now().plusDays(1),
                "Office", maxSeats, availableSeats, organizerId, LocalDateTime.now());
    }
}
