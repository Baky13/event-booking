package com.example.eventbooking.repository;

import com.example.eventbooking.model.Booking;
import com.example.eventbooking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByEventIdAndUserIdAndStatusNot(Long eventId, Long userId, BookingStatus status);

    // Баг 5 исправлен: JOIN FETCH предотвращает LazyInitializationException
    @Query("SELECT b FROM Booking b JOIN FETCH b.event WHERE b.userId = :userId AND b.status != :status")
    List<Booking> findByUserIdAndStatusNotWithEvent(@Param("userId") Long userId, @Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.event.id = :eventId AND b.status = :status ORDER BY b.waitlistPosition ASC")
    List<Booking> findByEventIdAndStatusOrderByWaitlistPosition(@Param("eventId") Long eventId, @Param("status") BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.userId = :userId AND b.status = 'CONFIRMED'")
    long countActiveBookingsByUserId(@Param("userId") Long userId);
}
