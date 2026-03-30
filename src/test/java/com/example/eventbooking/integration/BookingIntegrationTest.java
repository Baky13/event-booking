package com.example.eventbooking.integration;

import com.example.eventbooking.config.TestcontainersConfig;
import com.example.eventbooking.dto.*;
import com.example.eventbooking.model.BookingStatus;
import com.example.eventbooking.model.Event;
import com.example.eventbooking.model.User;
import com.example.eventbooking.repository.BookingRepository;
import com.example.eventbooking.repository.EventRepository;
import com.example.eventbooking.repository.UserRepository;
import com.example.eventbooking.service.AuthService;
import com.example.eventbooking.service.BookingService;
import com.example.eventbooking.service.EventService;
import com.example.eventbooking.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@Import(TestcontainersConfig.class)
class BookingIntegrationTest {

    @Autowired private EventService eventService;
    @Autowired private BookingService bookingService;
    @Autowired private AuthService authService;
    @Autowired private EventRepository eventRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Тест 1: fullBookingFlow
    @Test
    void fullBookingFlow_RegisterCreateBookCancelVerify() {
        // Given
        Long userId = createUser("user1@test.com");
        Long organizerId = createUser("org1@test.com");
        EventResponse event = eventService.createEvent(
                new CreateEventRequest("Java Meetup", "Desc", LocalDateTime.now().plusDays(3), "Office", 10),
                organizerId);

        // When: бронируем
        BookingResponse booking = bookingService.bookEvent(event.getId(), userId);

        // Then: место уменьшилось
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getAvailableSeats()).isEqualTo(9);

        // When: отменяем
        bookingService.cancelBooking(booking.getBookingId(), userId);

        // Then: место освободилось
        Event updated = eventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getAvailableSeats()).isEqualTo(10);
    }

    // Тест 2: overbookingProtection
    @Test
    void overbookingProtection_ThirdBookingRejected() {
        // Given
        Long user1 = createUser("u1@test.com");
        Long user2 = createUser("u2@test.com");
        Long user3 = createUser("u3@test.com");
        Long org = createUser("org2@test.com");
        EventResponse event = eventService.createEvent(
                new CreateEventRequest("Small Event", "Desc", LocalDateTime.now().plusDays(3), "Office", 2),
                org);

        // When
        bookingService.bookEvent(event.getId(), user1);
        bookingService.bookEvent(event.getId(), user2);

        // Then
        assertThatThrownBy(() -> bookingService.bookEvent(event.getId(), user3))
                .isInstanceOf(NoSeatsAvailableException.class);
    }

    // Тест 3: waitlistPromotion
    @Test
    void waitlistPromotion_CancelPromotesFirstInWaitlist() {
        // Given
        Long user1 = createUser("w1@test.com");
        Long user2 = createUser("w2@test.com");
        Long user3 = createUser("w3@test.com");
        Long org = createUser("org3@test.com");
        EventResponse event = eventService.createEvent(
                new CreateEventRequest("Full Event", "Desc", LocalDateTime.now().plusDays(3), "Office", 2),
                org);

        BookingResponse b1 = bookingService.bookEvent(event.getId(), user1);
        bookingService.bookEvent(event.getId(), user2);
        BookingResponse waitlist = bookingService.joinWaitlist(event.getId(), user3);

        assertThat(waitlist.getStatus()).isEqualTo(BookingStatus.WAITLISTED);
        assertThat(waitlist.getWaitlistPosition()).isEqualTo(1);

        // When: user1 отменяет
        bookingService.cancelBooking(b1.getBookingId(), user1);

        // Then: user3 получил место
        var promoted = bookingRepository.findById(waitlist.getBookingId()).orElseThrow();
        assertThat(promoted.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    // Тест 4: cancellationDeadline
    @Test
    void cancellationDeadline_EventIn2Hours_CannotCancel() {
        // Given
        Long userId = createUser("cd1@test.com");
        Long org = createUser("org4@test.com");
        EventResponse event = eventService.createEvent(
                new CreateEventRequest("Soon Event", "Desc", LocalDateTime.now().plusHours(2), "Office", 10),
                org);
        BookingResponse booking = bookingService.bookEvent(event.getId(), userId);

        // When & Then
        assertThatThrownBy(() -> bookingService.cancelBooking(booking.getBookingId(), userId))
                .isInstanceOf(CancellationDeadlineException.class);
    }

    // Тест 5: bookingLimit
    @Test
    void bookingLimit_6thBookingRejectedThenAllowedAfterCancel() {
        // Given
        Long userId = createUser("bl1@test.com");
        Long org = createUser("org5@test.com");

        // Создаём 5 мероприятий и бронируем
        Long firstBookingId = null;
        for (int i = 1; i <= 5; i++) {
            EventResponse event = eventService.createEvent(
                    new CreateEventRequest("Event " + i, "Desc", LocalDateTime.now().plusDays(i + 5), "Office", 10),
                    org);
            BookingResponse b = bookingService.bookEvent(event.getId(), userId);
            if (i == 1) firstBookingId = b.getBookingId();
        }

        // 6-е мероприятие
        EventResponse event6 = eventService.createEvent(
                new CreateEventRequest("Event 6", "Desc", LocalDateTime.now().plusDays(10), "Office", 10),
                org);

        // When: 6-е бронирование отклонено
        final Long eventId6 = event6.getId();
        assertThatThrownBy(() -> bookingService.bookEvent(eventId6, userId))
                .isInstanceOf(BookingLimitExceededException.class);

        // When: отменяем первое
        bookingService.cancelBooking(firstBookingId, userId);

        // Then: теперь 6-е проходит
        BookingResponse b6 = bookingService.bookEvent(eventId6, userId);
        assertThat(b6.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    private Long createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setName("Test User");
        user.setPassword(passwordEncoder.encode("password"));
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user).getId();
    }
}
