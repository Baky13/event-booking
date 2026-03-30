package com.example.eventbooking.repository;

import com.example.eventbooking.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOrganizerId(Long organizerId);

    @Query("SELECT e FROM Event e WHERE e.eventDate > :now ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE e.eventDate > :now ORDER BY e.eventDate ASC")
    Page<Event> findUpcomingEventsPaged(@Param("now") LocalDateTime now, Pageable pageable);
}
