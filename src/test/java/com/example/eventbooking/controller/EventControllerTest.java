package com.example.eventbooking.controller;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createEvent_ValidData_ReturnsCreated() throws Exception {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Java Meetup",
                "Meeting for Java developers",
                LocalDateTime.now().plusDays(1),
                "Office",
                10
        );

        EventResponse response = new EventResponse(
                1L,
                "Java Meetup",
                "Meeting for Java developers",
                LocalDateTime.now().plusDays(1),
                "Office",
                10,
                10,
                1L,
                LocalDateTime.now()
        );

        when(eventService.createEvent(any(), any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/events")
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
                "Java Meetup",
                "Meeting for Java developers",
                LocalDateTime.now().plusDays(1),
                "Office",
                10
        );

        // When & Then
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUpcomingEvents_Returns200() throws Exception {
        // Given
        List<EventResponse> events = List.of(
                new EventResponse(1L, "Event 1", "Description", LocalDateTime.now().plusDays(1), "Office", 10, 10, 1L, LocalDateTime.now()),
                new EventResponse(2L, "Event 2", "Description", LocalDateTime.now().plusDays(2), "Office", 5, 5, 2L, LocalDateTime.now())
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
        List<EventResponse> events = List.of(
                new EventResponse(1L, "My Event", "Description", LocalDateTime.now().plusDays(1), "Office", 10, 10, 1L, LocalDateTime.now())
        );

        when(eventService.getMyEvents(any())).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/api/events/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("My Event"));
    }

    @Test
    void getEventById_Exists_Returns200() throws Exception {
        // Given
        EventResponse event = new EventResponse(1L, "Event", "Description", LocalDateTime.now().plusDays(1), "Office", 10, 10, 1L, LocalDateTime.now());

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
        when(eventService.getEventById(999L)).thenThrow(new RuntimeException("Event not found"));

        // When & Then
        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound());
    }
}
