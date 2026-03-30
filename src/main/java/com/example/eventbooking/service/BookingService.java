package com.example.eventbooking.service;

import com.example.eventbooking.dto.BookingResponse;
import com.example.eventbooking.exception.*;
import com.example.eventbooking.model.Booking;
import com.example.eventbooking.model.BookingStatus;
import com.example.eventbooking.model.Event;
import com.example.eventbooking.repository.BookingRepository;
import com.example.eventbooking.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BookingService {

    private static final int MAX_ACTIVE_BOOKINGS = 5;
    private static final int CANCELLATION_HOURS_BEFORE = 24;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", java.util.Locale.ROOT);

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;

    public BookingService(BookingRepository bookingRepository, EventRepository eventRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
    }

    // Правило 6: Пользователь может забронировать место
    // Правило 7: Нельзя забронировать, если мест нет
    // Правило 8: Нельзя забронировать одно мероприятие дважды
    // Правило 9: Нельзя бронировать прошедшее мероприятие
    // Правило 10: Максимум 5 активных бронирований на пользователя
    @Transactional
    public BookingResponse bookEvent(Long eventId, Long userId) {
        Event event = findEventOrThrow(eventId);

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new EventExpiredException("Cannot book a past event");
        }

        bookingRepository.findByEventIdAndUserIdAndStatusNot(eventId, userId, BookingStatus.CANCELLED)
                .ifPresent(b -> { throw new DuplicateBookingException("Already booked this event"); });

        long activeBookings = bookingRepository.countActiveBookingsByUserId(userId);
        if (activeBookings >= MAX_ACTIVE_BOOKINGS) {
            throw new BookingLimitExceededException("Booking limit of 5 exceeded");
        }

        if (event.getAvailableSeats() <= 0) {
            throw new NoSeatsAvailableException("No seats available for event: " + event.getTitle());
        }

        event.setAvailableSeats(event.getAvailableSeats() - 1);
        eventRepository.save(event);

        Booking booking = new Booking();
        booking.setEvent(event);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        return toResponse(saved, event);
    }

    // Правило 11: Отмена возможна минимум за 24 часа
    // Правило 12: При отмене освобождается место
    // Правило 13: Нельзя отменить чужое бронирование
    // Правило 15: При отмене — первый из листа ожидания получает место
    @Transactional
    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EventNotFoundException("Booking not found"));

        if (!booking.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cannot cancel another user's booking");
        }

        Event event = booking.getEvent();
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(CANCELLATION_HOURS_BEFORE))) {
            throw new CancellationDeadlineException("Cannot cancel less than 24 hours before event");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        promoteFromWaitlistOrFreeSeats(event);
    }

    // Правило 14: Если мест нет — можно встать в лист ожидания
    // Правило 16: Нельзя быть одновременно в бронировании и в листе ожидания
    @Transactional
    public BookingResponse joinWaitlist(Long eventId, Long userId) {
        Event event = findEventOrThrow(eventId);

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new EventExpiredException("Cannot join waitlist for a past event");
        }

        bookingRepository.findByEventIdAndUserIdAndStatusNot(eventId, userId, BookingStatus.CANCELLED)
                .ifPresent(b -> { throw new DuplicateBookingException("Already booked or in waitlist for this event"); });

        List<Booking> waitlist = bookingRepository.findByEventIdAndStatusOrderByWaitlistPosition(
                eventId, BookingStatus.WAITLISTED);
        int position = waitlist.size() + 1;

        Booking booking = new Booking();
        booking.setEvent(event);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.WAITLISTED);
        booking.setWaitlistPosition(position);
        booking.setCreatedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        return toResponse(saved, event);
    }

    // Правило 17: Можно выйти из листа ожидания в любой момент
    @Transactional
    public void leaveWaitlist(Long eventId, Long userId) {
        Booking booking = bookingRepository.findByEventIdAndUserIdAndStatusNot(eventId, userId, BookingStatus.CANCELLED)
                .filter(b -> b.getStatus() == BookingStatus.WAITLISTED)
                .orElseThrow(() -> new EventNotFoundException("Waitlist entry not found"));

        int removedPosition = booking.getWaitlistPosition();
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        recalculateWaitlistPositions(eventId, removedPosition);
    }

    public List<BookingResponse> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdAndStatusNot(userId, BookingStatus.CANCELLED)
                .stream()
                .map(b -> toResponse(b, b.getEvent()))
                .toList();
    }

    private void promoteFromWaitlistOrFreeSeats(Event event) {
        List<Booking> waitlist = bookingRepository.findByEventIdAndStatusOrderByWaitlistPosition(
                event.getId(), BookingStatus.WAITLISTED);

        if (!waitlist.isEmpty()) {
            Booking first = waitlist.get(0);
            first.setStatus(BookingStatus.CONFIRMED);
            first.setWaitlistPosition(null);
            bookingRepository.save(first);

            recalculateWaitlistPositions(event.getId(), 1);
        } else {
            event.setAvailableSeats(event.getAvailableSeats() + 1);
            eventRepository.save(event);
        }
    }

    private void recalculateWaitlistPositions(Long eventId, int fromPosition) {
        List<Booking> remaining = bookingRepository.findByEventIdAndStatusOrderByWaitlistPosition(
                eventId, BookingStatus.WAITLISTED);
        int pos = fromPosition;
        for (Booking b : remaining) {
            if (b.getWaitlistPosition() > fromPosition - 1) {
                b.setWaitlistPosition(pos++);
                bookingRepository.save(b);
            }
        }
    }

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));
    }

    private BookingResponse toResponse(Booking booking, Event event) {
        return new BookingResponse(
                booking.getId(),
                event.getTitle(),
                booking.getStatus(),
                event.getAvailableSeats(),
                booking.getWaitlistPosition(),
                booking.getCreatedAt().format(FORMATTER)
        );
    }
}
