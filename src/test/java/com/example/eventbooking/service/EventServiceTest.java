package com.example.eventbooking.service;

import com.example.eventbooking.dto.CreateEventRequest;
import com.example.eventbooking.dto.EventResponse;
import com.example.eventbooking.exception.AccessDeniedException;
import com.example.eventbooking.exception.EventNotFoundException;
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
import java.util.Optional;

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
                "Java Meetup", "Meeting for Java developers",
                LocalDateTime.now().plusDays(1), "Office", 10);
        when(eventRepository.save(any())).thenAnswer(inv -> {
            Event event = inv.getArgument(0);
            event.setId(1L);
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        // When
        EventResponse response = eventService.createEvent(request, 1L);

        // Then
        assertThat(response.getTitle()).isEqualTo("Java Meetup");
        assertThat(response.getMaxSeats()).isEqualTo(10);
        assertThat(response.getAvailableSeats()).isEqualTo(10);
        verify(eventRepository).save(any());
    }

    // Правило 2: Дата мероприятия должна быть в будущем
    @Test
    void createEvent_DateInPast_ThrowsValidationException() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
                "Past Event", "Desc", LocalDateTime.now().minusDays(1), "Office", 10);

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
                "No Seats", "Desc", LocalDateTime.now().plusDays(1), "Office", 0);

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
        Event event1 = mockEvent(1L, 1L, 10, 10);
        Event event2 = mockEvent(2L, 1L, 10, 10);
        Event event3 = mockEvent(3L, 2L, 10, 10);
        when(eventRepository.findByOrganizerId(1L)).thenReturn(List.of(event1, event2));
        when(eventRepository.findByOrganizerId(2L)).thenReturn(List.of(event3));

        // When
        List<EventResponse> user1Events = eventService.getMyEvents(1L);
        List<EventResponse> user2Events = eventService.getMyEvents(2L);

        // Then
        assertThat(user1Events).hasSize(2);
        assertThat(user2Events).hasSize(1);
    }

    // Правило 5: Все пользователи видят список предстоящих мероприятий
    @Test
    void getUpcomingEvents_ReturnsOnlyFutureEvents() {
        // Given
        when(eventRepository.findUpcomingEvents(any()))
                .thenReturn(List.of(mockEvent(1L, 1L, 10, 10), mockEvent(2L, 2L, 10, 10)));

        // When
        List<EventResponse> events = eventService.getUpcomingEvents();

        // Then
        assertThat(events).hasSize(2);
        verify(eventRepository).findUpcomingEvents(any());
    }

    @Test
    void getEventById_ExistingId_ReturnsEvent() {
        // Given
        Event event = mockEvent(1L, 1L, 10, 10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // When
        EventResponse response = eventService.getEventById(1L);

        // Then
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getEventById_NotFound_ThrowsEventNotFoundException() {
        // Given
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.getEventById(99L))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void updateEvent_IncreasedMaxSeats_RecalculatesAvailableSeats() {
        // Given: maxSeats=10, availableSeats=7 (3 booked)
        Event event = mockEvent(1L, 1L, 10, 7);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CreateEventRequest request = new CreateEventRequest(
                "Updated", "Desc", LocalDateTime.now().plusDays(2), "Office", 20);

        // When
        EventResponse response = eventService.updateEvent(1L, request, 1L);

        // Then
        assertThat(response.getMaxSeats()).isEqualTo(20);
        assertThat(response.getAvailableSeats()).isEqualTo(17); // 7 + (20-10)
    }

    @Test
    void updateEvent_ReduceCapacityBelowBooked_ThrowsValidationException() {
        // Given: maxSeats=10, availableSeats=2 (8 booked)
        Event event = mockEvent(1L, 1L, 10, 2);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        CreateEventRequest request = new CreateEventRequest(
                "Updated", "Desc", LocalDateTime.now().plusDays(2), "Office", 5);

        // When & Then
        assertThatThrownBy(() -> eventService.updateEvent(1L, request, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Cannot reduce capacity below booked count");
    }

    @Test
    void updateEvent_NotOrganizer_ThrowsAccessDeniedException() {
        // Given
        Event event = mockEvent(1L, 1L, 10, 10); // organizer = 1
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        CreateEventRequest request = new CreateEventRequest(
                "Updated", "Desc", LocalDateTime.now().plusDays(2), "Office", 10);

        // When & Then
        assertThatThrownBy(() -> eventService.updateEvent(1L, request, 2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteEvent_Organizer_DeletesSuccessfully() {
        // Given
        Event event = mockEvent(1L, 1L, 10, 10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // When
        eventService.deleteEvent(1L, 1L);

        // Then
        verify(eventRepository).delete(event);
    }

    @Test
    void deleteEvent_NotOrganizer_ThrowsAccessDeniedException() {
        // Given
        Event event = mockEvent(1L, 1L, 10, 10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // When & Then
        assertThatThrownBy(() -> eventService.deleteEvent(1L, 2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void bookEvent_OrganizerBooksOwnEvent_ThrowsValidationException() {
        // Given: organizerId = 99L (из eventWith), userId = 99L
        Event event = mockEvent(1L, 99L, 10, 10);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // When & Then
        assertThatThrownBy(() -> eventService.bookOwnEvent(1L, 99L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("organizer cannot book");
    }

    private Event mockEvent(Long id, Long organizerId, int maxSeats, int availableSeats) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Event " + id);
        event.setDescription("Description");
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setLocation("Office");
        event.setMaxSeats(maxSeats);
        event.setAvailableSeats(availableSeats);
        event.setOrganizerId(organizerId);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}
