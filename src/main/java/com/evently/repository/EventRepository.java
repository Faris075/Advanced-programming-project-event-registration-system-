package com.evently.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.evently.model.Event;
import com.evently.model.EventStatus;

/**
 * Data access for the `events` table.
 *
 * OWNER: Faris USED BY: Mohamed Morsy (AdminEventController, EventScheduler)
 * Mohamed Ehab (PublicEventController) Mohamed Ahmed (RegistrationService)
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Public listing – only PUBLISHED events, newest first.
     */
    Page<Event> findByStatusOrderByDateTimeAsc(EventStatus status, Pageable pageable);

    /**
     * Public listing without pagination – returns all matching events as a list.
     */
    List<Event> findByStatusOrderByDateTimeAsc(EventStatus status);

    long countByStatus(EventStatus status);

    List<Event> findTop5ByOrderByCreatedAtDesc();

    /**
     * Used by the daily scheduler to find events that should be auto-completed.
     * Returns PUBLISHED events whose start time is in the past.
     */
    List<Event> findByStatusAndDateTimeBefore(EventStatus status, LocalDateTime dateTime);

    /**
     * Counts confirmed registrations for capacity enforcement. Kept here (not
     * in RegistrationRepository) so it's easy to find from EventService.
     *
     * NOTE: Use the fully-qualified enum reference in JPQL — string literals
     * like 'CONFIRMED' are not valid JPQL and will cause a query parsing error
     * at startup.
     */
    @Query("SELECT COUNT(r) FROM Registration r "
            + "WHERE r.event.id = :eventId AND r.status = com.evently.model.RegistrationStatus.CONFIRMED")
    long countConfirmedRegistrations(@Param("eventId") Long eventId);

    /**
     * Counts admin-override registrations for the 5-override cap check.
     */
    @Query("SELECT COUNT(r) FROM Registration r "
            + "WHERE r.event.id = :eventId AND r.isAdminOverride = true")
    long countAdminOverrides(@Param("eventId") Long eventId);
}
