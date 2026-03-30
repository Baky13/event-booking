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
import java.util.Locale;

@Service
public class BookingService {

    private static final int MAX_ACTIVE_BOOKINGS = 5;
    private static final int CANCELLATION_HOURS_BEFORE = 24;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT);

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;

    public BookingService(BookingRepository bookingRepository, EventRepository eventRepository) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
    }

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
        return toResponse(bookingRepository.save(booking), event);
    }

    // Баг 1 исправлен: отмена по eventId — сервис сам находит бронирование пользователя
    @Transactional
    public void cancelBookingByEventId(Long eventId, Long userId) {
        Booking booking = bookingRepository
                .findByEventIdAndUserIdAndStatusNot(eventId, userId, BookingStatus.CANCELLED)
                .orElseThrow(() -> new EventNotFoundException("Booking not found for this event"));

        Event event = booking.getEvent();
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(CANCELLATION_HOURS_BEFORE))) {
            throw new CancellationDeadlineException("Cannot cancel less than 24 hours before event");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        promoteFromWaitlistOrFreeSeats(event);
    }

    // Баг 2 исправлен: joinWaitlist проверяет что мест нет
    @Transactional
    public BookingResponse joinWaitlist(Long eventId, Long userId) {
        Event event = findEventOrThrow(eventId);

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new EventExpiredException("Cannot join waitlist for a past event");
        }

        bookingRepository.findByEventIdAndUserIdAndStatusNot(eventId, userId, BookingStatus.CANCELLED)
                .ifPresent(b -> { throw new DuplicateBookingException("Already booked or in waitlist for this event"); });

        if (event.getAvailableSeats() > 0) {
            throw new ValidationException("Seats are available — use booking instead of waitlist");
        }

        List<Booking> waitlist = bookingRepository.findByEventIdAndStatusOrderByWaitlistPosition(
                eventId, BookingStatus.WAITLISTED);

        Booking booking = new Booking();
        booking.setEvent(event);
        booking.setUserId(userId);
        booking.setStatus(BookingStatus.WAITLISTED);
        booking.setWaitlistPosition(waitlist.size() + 1);
        booking.setCreatedAt(LocalDateTime.now());
        return toResponse(bookingRepository.save(booking), event);
    }

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

    // Баг 5 исправлен: @Transactional(readOnly = true) + JOIN FETCH в репозитории
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdAndStatusNotWithEvent(userId, BookingStatus.CANCELLED)
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
