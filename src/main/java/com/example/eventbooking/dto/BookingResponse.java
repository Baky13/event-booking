package com.example.eventbooking.dto;

import com.example.eventbooking.model.BookingStatus;

public class BookingResponse {
    private Long bookingId;
    private String eventTitle;
    private BookingStatus status;
    private Integer availableSeats;
    private Integer waitlistPosition;
    private String bookedAt;

    public BookingResponse() {}

    public BookingResponse(Long bookingId, String eventTitle, BookingStatus status,
                           Integer availableSeats, Integer waitlistPosition, String bookedAt) {
        this.bookingId = bookingId;
        this.eventTitle = eventTitle;
        this.status = status;
        this.availableSeats = availableSeats;
        this.waitlistPosition = waitlistPosition;
        this.bookedAt = bookedAt;
    }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }

    public Integer getWaitlistPosition() { return waitlistPosition; }
    public void setWaitlistPosition(Integer waitlistPosition) { this.waitlistPosition = waitlistPosition; }

    public String getBookedAt() { return bookedAt; }
    public void setBookedAt(String bookedAt) { this.bookedAt = bookedAt; }
}
