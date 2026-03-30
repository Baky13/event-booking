package com.example.eventbooking.service;

import com.example.eventbooking.dto.BookingResponse;
import com.example.eventbooking.exception.*;
import com.example.eventbooking.model.Booking;
import com.example.eventbooking.model.BookingStatus;
import com.example.eventbooking.model.Event;
import com.example.eventbooking.repository.BookingRepository;
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
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, eventRepository);
    }

    // Правило 6: Пользователь может забронировать место
    @Test
    void bookEvent_AvailableSeats_ReturnsBookingConfirmed() {
        // Given
        Event event = eventWith(1L, 10, LocalDateTime.now().plusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(1L, 1L, BookingStatus.CANCELLED))
                .thenReturn(Optional.empty());
        when(bookingRepository.countActiveBookingsByUserId(1L)).thenReturn(0L);
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        // When
        BookingResponse response = bookingService.bookEvent(1L, 1L);

        // Then
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.getAvailableSeats()).isEqualTo(9);
        verify(eventRepository).save(event);
    }

    // Правило 6: Количество доступных мест уменьшается на 1
    @Test
    void bookEvent_AvailableSeats_DecreasesAvailableCount() {
        // Given
        Event event = eventWith(1L, 10, LocalDateTime.now().plusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(any(), any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.countActiveBookingsByUserId(any())).thenReturn(0L);
        when(bookingRepository.save(any())).thenAnswer(inv -> { Booking b = inv.getArgument(0); b.setId(1L); return b; });

        // When
        bookingService.bookEvent(1L, 1L);

        // Then
        assertThat(event.getAvailableSeats()).isEqualTo(9);
    }

    // Правило 6: Последнее место
    @Test
    void bookEvent_LastSeat_AvailableSeatsBecomesZero() {
        // Given
        Event event = eventWith(1L, 1, LocalDateTime.now().plusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(any(), any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.countActiveBookingsByUserId(any())).thenReturn(0L);
        when(bookingRepository.save(any())).thenAnswer(inv -> { Booking b = inv.getArgument(0); b.setId(1L); return b; });

        // When
        bookingService.bookEvent(1L, 1L);

        // Then
        assertThat(event.getAvailableSeats()).isEqualTo(0);
    }

    // Правило 7: Нельзя забронировать, если мест нет
    @Test
    void bookEvent_NoSeatsLeft_ThrowsNoSeatsAvailableException() {
        // Given
        Event event = eventWith(1L, 0, LocalDateTime.now().plusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(any(), any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.countActiveBookingsByUserId(any())).thenReturn(0L);

        // When & Then
        assertThatThrownBy(() -> bookingService.bookEvent(1L, 1L))
                .isInstanceOf(NoSeatsAvailableException.class)
                .hasMessageContaining("No seats available");
    }

    // Правило 8: Нельзя забронировать одно мероприятие дважды
    @Test
    void bookEvent_AlreadyBooked_ThrowsDuplicateBookingException() {
        // Given
        Event event = eventWith(1L, 10, LocalDateTime.now().plusDays(1));
        Booking existing = new Booking();
        existing.setStatus(BookingStatus.CONFIRMED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(1L, 1L, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(existing));

        // When & Then
        assertThatThrownBy(() -> bookingService.bookEvent(1L, 1L))
                .isInstanceOf(DuplicateBookingException.class)
                .hasMessageContaining("Already booked");
    }

    // Правило 9: Нельзя бронировать прошедшее мероприятие
    @Test
    void bookEvent_PastEvent_ThrowsEventExpiredException() {
        // Given
        Event event = eventWith(1L, 10, LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // When & Then
        assertThatThrownBy(() -> bookingService.bookEvent(1L, 1L))
                .isInstanceOf(EventExpiredException.class)
                .hasMessageContaining("past event");
    }

    // Правило 10: Максимум 5 активных бронирований
    @Test
    void bookEvent_UserHas5Bookings_ThrowsBookingLimitExceededException() {
        // Given
        Event event = eventWith(1L, 10, LocalDateTime.now().plusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(any(), any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.countActiveBookingsByUserId(1L)).thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> bookingService.bookEvent(1L, 1L))
                .isInstanceOf(BookingLimitExceededException.class)
                .hasMessageContaining("limit");
    }

    // Правило 10: Отменённые не считаются
    @Test
    void bookEvent_UserHas5But2Cancelled_AllowsBooking() {
        // Given
        Event event = eventWith(1L, 10, LocalDateTime.now().plusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(any(), any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.countActiveBookingsByUserId(1L)).thenReturn(3L);
        when(bookingRepository.save(any())).thenAnswer(inv -> { Booking b = inv.getArgument(0); b.setId(1L); return b; });

        // When
        BookingResponse response = bookingService.bookEvent(1L, 1L);

        // Then
        assertThat(response.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    // Правило 11: Отмена возможна минимум за 24 часа
    @Test
    void cancelBooking_MoreThan24Hours_Success() {
        // Given
        Event event = eventWith(1L, 9, LocalDateTime.now().plusDays(2));
        Booking booking = bookingWith(1L, 1L, event, BookingStatus.CONFIRMED);
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(1L, 1L, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.findByEventIdAndStatusOrderByWaitlistPosition(any(), any())).thenReturn(List.of());

        // When
        bookingService.cancelBookingByEventId(1L, 1L);

        // Then
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isNotNull();
    }

    @Test
    void cancelBooking_LessThan24Hours_ThrowsCancellationDeadlineException() {
        // Given
        Event event = eventWith(1L, 9, LocalDateTime.now().plusHours(12));
        Booking booking = bookingWith(1L, 1L, event, BookingStatus.CONFIRMED);
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(1L, 1L, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(booking));

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBookingByEventId(1L, 1L))
                .isInstanceOf(CancellationDeadlineException.class)
                .hasMessageContaining("24 hours");
    }

    // Правило 12: При отмене освобождается место
    @Test
    void cancelBooking_SeatBecomesAvailable() {
        // Given
        Event event = eventWith(1L, 9, LocalDateTime.now().plusDays(2));
        Booking booking = bookingWith(1L, 1L, event, BookingStatus.CONFIRMED);
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(1L, 1L, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.findByEventIdAndStatusOrderByWaitlistPosition(any(), any())).thenReturn(List.of());

        // When
        bookingService.cancelBookingByEventId(1L, 1L);

        // Then
        assertThat(event.getAvailableSeats()).isEqualTo(10);
        verify(eventRepository).save(event);
    }

    // Правило 13: Нельзя отменить чужое бронирование
    @Test
    void cancelBooking_NotOwner_ThrowsAccessDeniedException() {
        // Given
        Event event = eventWith(1L, 9, LocalDateTime.now().plusDays(2));
        Booking booking = bookingWith(1L, 2L, event, BookingStatus.CONFIRMED);
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(1L, 1L, BookingStatus.CANCELLED))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBookingByEventId(1L, 1L))
                .isInstanceOf(EventNotFoundException.class);
    }

    // Правило 14: Если мест нет — можно встать в лист ожидания
    @Test
    void joinWaitlist_NoSeats_ReturnsWaitlistPosition() {
        // Given
        Event event = eventWith(1L, 0, LocalDateTime.now().plusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(any(), any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.findByEventIdAndStatusOrderByWaitlistPosition(1L, BookingStatus.WAITLISTED))
                .thenReturn(List.of());
        when(bookingRepository.save(any())).thenAnswer(inv -> { Booking b = inv.getArgument(0); b.setId(1L); return b; });

        // When
        BookingResponse response = bookingService.joinWaitlist(1L, 1L);

        // Then
        assertThat(response.getStatus()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(response.getWaitlistPosition()).isEqualTo(1);
    }

    // Правило 14: Нельзя встать в очередь если есть места
    @Test
    void joinWaitlist_SeatsAvailable_ThrowsValidationException() {
        // Given
        Event event = eventWith(1L, 5, LocalDateTime.now().plusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(any(), any(), any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookingService.joinWaitlist(1L, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Seats are available");
    }

    // Правило 15: При отмене — первый из листа ожидания получает место
    @Test
    void cancelBooking_WaitlistExists_FirstInWaitlistGetsBooking() {
        // Given
        Event event = eventWith(1L, 0, LocalDateTime.now().plusDays(2));
        Booking cancelledBooking = bookingWith(1L, 1L, event, BookingStatus.CONFIRMED);
        Booking waitlistUser = bookingWith(2L, 3L, event, BookingStatus.WAITLISTED);
        waitlistUser.setWaitlistPosition(1);
        Booking waitlistUser2 = bookingWith(3L, 4L, event, BookingStatus.WAITLISTED);
        waitlistUser2.setWaitlistPosition(2);

        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(1L, 1L, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(cancelledBooking));
        when(bookingRepository.findByEventIdAndStatusOrderByWaitlistPosition(1L, BookingStatus.WAITLISTED))
                .thenReturn(List.of(waitlistUser, waitlistUser2))
                .thenReturn(List.of(waitlistUser2));

        // When
        bookingService.cancelBookingByEventId(1L, 1L);

        // Then
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(waitlistUser.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(waitlistUser.getWaitlistPosition()).isNull();
        assertThat(event.getAvailableSeats()).isEqualTo(0);
    }

    // Правило 16: Нельзя быть одновременно в бронировании и в листе ожидания
    @Test
    void joinWaitlist_AlreadyBooked_ThrowsDuplicateBookingException() {
        // Given
        Event event = eventWith(1L, 0, LocalDateTime.now().plusDays(1));
        Booking existing = new Booking();
        existing.setStatus(BookingStatus.CONFIRMED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(1L, 1L, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(existing));

        // When & Then
        assertThatThrownBy(() -> bookingService.joinWaitlist(1L, 1L))
                .isInstanceOf(DuplicateBookingException.class);
    }

    // Правило 17: Можно выйти из листа ожидания — позиции пересчитываются
    @Test
    void leaveWaitlist_Success_PositionsRecalculated() {
        // Given
        Event event = eventWith(1L, 0, LocalDateTime.now().plusDays(1));
        Booking waitlistEntry = bookingWith(1L, 1L, event, BookingStatus.WAITLISTED);
        waitlistEntry.setWaitlistPosition(1);
        Booking waitlistEntry2 = bookingWith(2L, 2L, event, BookingStatus.WAITLISTED);
        waitlistEntry2.setWaitlistPosition(2);

        when(bookingRepository.findByEventIdAndUserIdAndStatusNot(1L, 1L, BookingStatus.CANCELLED))
                .thenReturn(Optional.of(waitlistEntry));
        when(bookingRepository.findByEventIdAndStatusOrderByWaitlistPosition(1L, BookingStatus.WAITLISTED))
                .thenReturn(List.of(waitlistEntry2));

        // When
        bookingService.leaveWaitlist(1L, 1L);

        // Then
        assertThat(waitlistEntry.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(waitlistEntry.getCancelledAt()).isNotNull();
        assertThat(waitlistEntry2.getWaitlistPosition()).isEqualTo(1);
    }

    private Event eventWith(Long id, int availableSeats, LocalDateTime eventDate) {
        Event event = new Event();
        event.setId(id);
        event.setTitle("Test Event");
        event.setAvailableSeats(availableSeats);
        event.setMaxSeats(10);
        event.setEventDate(eventDate);
        event.setLocation("Office");
        event.setOrganizerId(99L);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private Booking bookingWith(Long id, Long userId, Event event, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUserId(userId);
        booking.setEvent(event);
        booking.setStatus(status);
        booking.setCreatedAt(LocalDateTime.now());
        return booking;
    }
}
