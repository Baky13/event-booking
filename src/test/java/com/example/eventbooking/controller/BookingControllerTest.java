package com.example.eventbooking.controller;

import com.example.eventbooking.config.SecurityConfig;
import com.example.eventbooking.dto.BookingResponse;
import com.example.eventbooking.exception.*;
import com.example.eventbooking.model.BookingStatus;
import com.example.eventbooking.security.JwtTokenProvider;
import com.example.eventbooking.security.UserPrincipal;
import com.example.eventbooking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.example.eventbooking.security.JwtBlacklist jwtBlacklist;

    private UsernamePasswordAuthenticationToken userAuth() {
        UserPrincipal principal = new UserPrincipal(1L, "user@test.com");
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }

    @Test
    void bookEvent_AvailableSeats_Returns201() throws Exception {
        // Given
        BookingResponse response = new BookingResponse(1L, "Java Meetup", BookingStatus.CONFIRMED, 9, null, "25.03.2026 15:00");
        when(bookingService.bookEvent(1L, 1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/events/1/book").with(authentication(userAuth())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.availableSeats").value(9));
    }

    @Test
    void bookEvent_Unauthorized_Returns401() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/events/1/book"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bookEvent_NoSeats_Returns400() throws Exception {
        // Given
        when(bookingService.bookEvent(any(), any())).thenThrow(new NoSeatsAvailableException("No seats available"));

        // When & Then
        mockMvc.perform(post("/api/events/1/book").with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No seats available"));
    }

    @Test
    void bookEvent_AlreadyBooked_Returns400() throws Exception {
        // Given
        when(bookingService.bookEvent(any(), any())).thenThrow(new DuplicateBookingException("Already booked this event"));

        // When & Then
        mockMvc.perform(post("/api/events/1/book").with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Already booked this event"));
    }

    @Test
    void bookEvent_PastEvent_Returns400() throws Exception {
        // Given
        when(bookingService.bookEvent(any(), any())).thenThrow(new EventExpiredException("Cannot book a past event"));

        // When & Then
        mockMvc.perform(post("/api/events/1/book").with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot book a past event"));
    }

    @Test
    void bookEvent_LimitExceeded_Returns400() throws Exception {
        // Given
        when(bookingService.bookEvent(any(), any())).thenThrow(new BookingLimitExceededException("Booking limit of 5 exceeded"));

        // When & Then
        mockMvc.perform(post("/api/events/1/book").with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Booking limit of 5 exceeded"));
    }

    @Test
    void cancelBooking_Success_Returns204() throws Exception {
        // Given
        doNothing().when(bookingService).cancelBookingByEventId(1L, 1L);

        // When & Then
        mockMvc.perform(delete("/api/events/1/book").with(authentication(userAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelBooking_LessThan24Hours_Returns400() throws Exception {
        // Given
        doThrow(new CancellationDeadlineException("Cannot cancel less than 24 hours before event"))
                .when(bookingService).cancelBookingByEventId(any(), any());

        // When & Then
        mockMvc.perform(delete("/api/events/1/book").with(authentication(userAuth())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot cancel less than 24 hours before event"));
    }

    @Test
    void cancelBooking_NotOwner_Returns403() throws Exception {
        // Given
        doThrow(new AccessDeniedException("Cannot cancel another user's booking"))
                .when(bookingService).cancelBookingByEventId(any(), any());

        // When & Then
        mockMvc.perform(delete("/api/events/1/book").with(authentication(userAuth())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Cannot cancel another user's booking"));
    }

    @Test
    void joinWaitlist_NoSeats_Returns200WithPosition() throws Exception {
        // Given
        BookingResponse response = new BookingResponse(2L, "Java Meetup", BookingStatus.WAITLISTED, 0, 1, "25.03.2026 16:00");
        when(bookingService.joinWaitlist(1L, 1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/events/1/waitlist").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1));
    }

    @Test
    void leaveWaitlist_Success_Returns204() throws Exception {
        // Given
        doNothing().when(bookingService).leaveWaitlist(1L, 1L);

        // When & Then
        mockMvc.perform(delete("/api/events/1/waitlist").with(authentication(userAuth())))
                .andExpect(status().isNoContent());
    }

    @Test
    void getMyBookings_Returns200() throws Exception {
        // Given
        List<BookingResponse> bookings = List.of(
                new BookingResponse(1L, "Event 1", BookingStatus.CONFIRMED, 5, null, "25.03.2026 15:00")
        );
        when(bookingService.getMyBookings(1L)).thenReturn(bookings);

        // When & Then
        mockMvc.perform(get("/api/bookings/my").with(authentication(userAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventTitle").value("Event 1"));
    }
}
