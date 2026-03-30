package com.example.eventbooking.service;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.exception.ValidationException;
import com.example.eventbooking.model.Event;
import com.example.eventbooking.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository);
    }

    // Правило 1: Организатор может создать мероприятие
    @Test
    void createEvent_ValidData_ReturnsCreatedEvent() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Java Meetup",
                "Meeting for Java developers",
                LocalDateTime.now().plusDays(1),
                "Office",
                10
        );

        when(eventRepository.save(any())).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(1L);
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // When
        EventResponse response = eventService.createEvent(request, 1L);

        // Then
        assertThat(response.getTitle()).isEqualTo("Java Meetup");
        assertThat(response.getDescription()).isEqualTo("Meeting for Java developers");
        assertThat(response.getLocation()).isEqualTo("Office");
        assertThat(response.getMaxSeats()).isEqualTo(10);
        assertThat(response.getAvailableSeats()).isEqualTo(10);
        assertThat(response.getOrganizerId()).isEqualTo(1L);
        verify(eventRepository).save(any());
    }

    // Правило 2: Дата мероприятия должна быть в будущем
    @Test
    void createEvent_DateInPast_ThrowsValidationException() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Past Event",
                "Event in the past",
                LocalDateTime.now().minusDays(1),
                "Office",
                10
        );

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(request, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Event date must be in the future");
        verify(eventRepository, never()).save(any());
    }

    // Правило 3: Максимальное количество мест ≥ 1
    @Test
    void createEvent_ZeroCapacity_ThrowsValidationException() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "No Seats Event",
                "Event with no seats",
                LocalDateTime.now().plusDays(1),
                "Office",
                0
        );

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(request, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Max seats must be at least 1");
        verify(eventRepository, never()).save(any());
    }

    // Правило 4: Организатор видит только свои мероприятия
    @Test
    void getMyEvents_ReturnsOnlyOwnEvents() {
        // Given
        Event event1 = createMockEvent(1L, 1L);
        Event event2 = createMockEvent(2L, 1L);
        Event event3 = createMockEvent(3L, 2L);

        when(eventRepository.findByOrganizerId(1L)).thenReturn(List.of(event1, event2));
        when(eventRepository.findByOrganizerId(2L)).thenReturn(List.of(event3));

        // When
        List<EventResponse> user1Events = eventService.getMyEvents(1L);
        List<EventResponse> user2Events = eventService.getMyEvents(2L);

        // Then
        assertThat(user1Events).hasSize(2);
        assertThat(user1Events.get(0).getOrganizerId()).isEqualTo(1L);
        assertThat(user1Events.get(1).getOrganizerId()).isEqualTo(1L);

        assertThat(user2Events).hasSize(1);
        assertThat(user2Events.get(0).getOrganizerId()).isEqualTo(2L);
    }

    // Правило 5: Все пользователи видят список предстоящих мероприятий
    @Test
    void getUpcomingEvents_ReturnsOnlyFutureEvents() {
        // Given
        Event event1 = createMockEvent(1L, 1L);
        Event event2 = createMockEvent(2L, 2L);

        when(eventRepository.findUpcomingEvents()).thenReturn(List.of(event1, event2));

        // When
        List<EventResponse> upcomingEvents = eventService.getUpcomingEvents();

        // Then
        assertThat(upcomingEvents).hasSize(2);
        verify(eventRepository).findUpcomingEvents();
    }

    private Event createMockEvent(Long id, Long organizerId) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Event " + id);
        event.setDescription("Description");
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setLocation("Office");
        event.setMaxSeats(10);
        event.setAvailableSeats(10);
        event.setOrganizerId(organizerId);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}
